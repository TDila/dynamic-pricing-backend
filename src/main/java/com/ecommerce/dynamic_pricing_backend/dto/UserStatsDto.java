package com.ecommerce.dynamic_pricing_backend.dto;

import lombok.Data;

@Data
public class UserStatsDto {
    private Long userId;
    private long orderCount;
    private boolean firstTimeBuyer;
}
