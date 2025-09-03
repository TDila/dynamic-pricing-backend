package com.ecommerce.dynamic_pricing_backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartDto {
    private Long id;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String appliedPromotionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
