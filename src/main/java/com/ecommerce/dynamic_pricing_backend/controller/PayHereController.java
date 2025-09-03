package com.ecommerce.dynamic_pricing_backend.controller;

import com.ecommerce.dynamic_pricing_backend.entity.Order;
import com.ecommerce.dynamic_pricing_backend.repository.OrderRepository;
import com.ecommerce.dynamic_pricing_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PayHereController {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @PostMapping("/notify")
    public ResponseEntity<String> handlePayHereNotify(@RequestParam Map<String, String> params) {
        String orderId = params.get("order_id");
        String paymentId = params.get("payment_id");
        String statusCode = params.get("status_code");

        boolean valid = paymentService.verifyMd5Signature(params);
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setPaymentId(paymentId);
        if ("2".equals(statusCode)) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
        } else {
            order.setStatus(Order.OrderStatus.FAILED);
        }
        orderRepository.save(order);

        return ResponseEntity.ok("OK");
    }

}

