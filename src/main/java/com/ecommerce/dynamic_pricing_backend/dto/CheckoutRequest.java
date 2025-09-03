package com.ecommerce.dynamic_pricing_backend.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String shippingAddress;
    private String paymentMethod;
    private String promotionCode;
}
