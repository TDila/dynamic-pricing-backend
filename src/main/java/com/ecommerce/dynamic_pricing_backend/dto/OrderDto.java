package com.ecommerce.dynamic_pricing_backend.dto;

import com.ecommerce.dynamic_pricing_backend.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OrderDto {
    private Long id;
    private String orderNumber;
    private List<OrderItemDto> orderItems;
    private Order.OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String paymentMethod;
    private String paymentId;
    private String appliedPromotionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, String> payhereParams;
}
