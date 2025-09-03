package com.ecommerce.dynamic_pricing_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    @Column(name = "condition_field")
    private String conditionField;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_operator")
    private ConditionOperator conditionOperator;

    @Column(name = "condition_value")
    private String conditionValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "priority")
    private Integer priority = 0; // Higher number = higher priority

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RuleType {
        CART_TOTAL, QUANTITY_BASED, CATEGORY_BASED, FIRST_TIME_BUYER, LOYALTY_DISCOUNT
    }

    public enum ConditionOperator {
        GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, EQUALS, CONTAINS
    }

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }
}
