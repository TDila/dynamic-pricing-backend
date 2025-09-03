package com.ecommerce.dynamic_pricing_backend.dto;

import com.ecommerce.dynamic_pricing_backend.entity.DiscountRule;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountRuleDto {
    private Long id;
    private String name;
    private String description;
    private DiscountRule.RuleType ruleType;
    private String conditionField;
    private DiscountRule.ConditionOperator conditionOperator;
    private String conditionValue;
    private DiscountRule.DiscountType discountType;
    private BigDecimal discountValue;
    private Integer priority;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
