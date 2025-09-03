package com.ecommerce.dynamic_pricing_backend.dto;

import com.ecommerce.dynamic_pricing_backend.entity.Promotion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePromotionRequest {
    @NotBlank(message = "Promotion name is required")
    private String name;

    private String description;

    @NotBlank(message = "Promotion code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private Promotion.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private String category;
    private String brand;
}
