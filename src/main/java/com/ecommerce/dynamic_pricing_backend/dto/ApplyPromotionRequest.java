package com.ecommerce.dynamic_pricing_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplyPromotionRequest {
    @NotBlank(message = "Promotion code is required")
    private String code;
}
