package com.ecommerce.dynamic_pricing_backend.dto;

import com.ecommerce.dynamic_pricing_backend.entity.Promotion;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionDto {
    private Long id;
    private String name;
    private String description;
    private String code;
    private Promotion.DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private String category;
    private String brand;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
