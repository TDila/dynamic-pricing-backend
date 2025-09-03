package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.CheckoutRequest;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    @Value("${payhere.sandbox:true}") // true for sandbox, false for live
    private boolean sandbox;

    @Value("${payhere.return-url}")
    private String returnUrl;

    @Value("${payhere.cancel-url}")
    private String cancelUrl;

    @Value("${payhere.notify-url}")
    private String notifyUrl;

    private final UserService userService;

    public Map<String, String> processPayment(Long userId, CheckoutRequest request, BigDecimal amount, String orderNumber) {
        try {
            User user = userService.findById(userId);

            String endpoint = sandbox
                    ? "https://sandbox.payhere.lk/pay/checkout"
                    : "https://www.payhere.lk/pay/checkout";

            Map<String, String> params = new HashMap<>();
            params.put("merchant_id", merchantId);
            params.put("return_url", returnUrl);
            params.put("cancel_url", cancelUrl);
            params.put("notify_url", notifyUrl);
            params.put("order_id", orderNumber);
            params.put("items", "Order for user " + userId);
            params.put("currency", "LKR");
            params.put("amount", amount.setScale(2, RoundingMode.HALF_UP).toString());

            params.put("first_name", user.getFirstName());
            params.put("last_name", user.getLastName());
            params.put("email", user.getEmail());
            params.put("phone", user.getPhone());
            params.put("address", request.getShippingAddress());
            params.put("city", "Colombo"); // Or extract from request

            // generate hash (merchant secret signing)
            String hash = generateMd5Hash(
                    merchantId + orderNumber + amount.setScale(2, RoundingMode.HALF_UP).toString() + "LKR" + merchantSecret
            );
            params.put("hash", hash);

            params.put("endpoint", endpoint);

            return params;

        } catch (Exception e) {
            log.error("PayHere payment failed: {}", e.getMessage());
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    private String generateMd5Hash(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, messageDigest);
        return String.format("%032x", number);
    }

    public boolean verifyMd5Signature(Map<String, String> params) {
        try {
            String orderId = params.get("order_id");
            String payhereAmount = params.get("payhere_amount");
            String payhereCurrency = params.get("payhere_currency");
            String receivedMd5Sig = params.get("md5sig");

            String raw = merchantId + orderId + payhereAmount + payhereCurrency + merchantSecret;
            String generatedHash = generateMd5Hash(raw);

            return generatedHash.equalsIgnoreCase(receivedMd5Sig);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void processRefund(String paymentId, BigDecimal amount) {
        // PayHere does not provide direct API refunds in sandbox
        log.warn("Refunds must be handled manually in PayHere dashboard.");
    }

    public boolean validatePaymentMethod(String paymentMethod) {
        return "PAYHERE".equalsIgnoreCase(paymentMethod) ||
                "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod);
    }


}
