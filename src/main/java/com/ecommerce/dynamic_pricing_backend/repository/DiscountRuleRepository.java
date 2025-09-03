package com.ecommerce.dynamic_pricing_backend.repository;

import com.ecommerce.dynamic_pricing_backend.entity.DiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    @Query("SELECT dr FROM DiscountRule dr WHERE dr.active = true AND " +
            "(dr.startDate IS NULL OR dr.startDate <= :now) AND " +
            "(dr.endDate IS NULL OR dr.endDate >= :now) " +
            "ORDER BY dr.priority DESC")
    List<DiscountRule> findActiveRules(@Param("now") LocalDateTime now);

    @Query("SELECT dr FROM DiscountRule dr WHERE dr.active = true AND dr.ruleType = :ruleType AND " +
            "(dr.startDate IS NULL OR dr.startDate <= :now) AND " +
            "(dr.endDate IS NULL OR dr.endDate >= :now) " +
            "ORDER BY dr.priority DESC")
    List<DiscountRule> findActiveRulesByType(@Param("ruleType") DiscountRule.RuleType ruleType,
                                             @Param("now") LocalDateTime now);

    List<DiscountRule> findByActiveTrue();

    List<DiscountRule> findByActiveTrueOrderByPriorityDesc();

    @Query("SELECT dr FROM DiscountRule dr WHERE dr.endDate < :now")
    List<DiscountRule> findExpiredRules(@Param("now") LocalDateTime now);
}
