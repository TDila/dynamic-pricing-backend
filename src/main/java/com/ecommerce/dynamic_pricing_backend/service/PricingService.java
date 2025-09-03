package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.CartDto;
import com.ecommerce.dynamic_pricing_backend.dto.PriceCalculationResult;
import com.ecommerce.dynamic_pricing_backend.entity.Cart;
import com.ecommerce.dynamic_pricing_backend.entity.DiscountRule;
import com.ecommerce.dynamic_pricing_backend.entity.Product;
import com.ecommerce.dynamic_pricing_backend.entity.Promotion;
import com.ecommerce.dynamic_pricing_backend.repository.DiscountRuleRepository;
import com.ecommerce.dynamic_pricing_backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.kie.api.runtime.KieSession;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final DiscountRuleRepository discountRuleRepository;
    private final PromotionRepository promotionRepository;
    private final KieSession kieSession;

    @Cacheable(value = "productPrices", key = "#product.id")
    public BigDecimal calculateProductPrice(Product product) {
        // Check for category or brand-specific promotions
        List<Promotion> promotions = promotionRepository.findActivePromotionsByCategory(
                product.getCategory(), LocalDateTime.now());

        promotions.addAll(promotionRepository.findActivePromotionsByBrand(
                product.getBrand(), LocalDateTime.now()));

        BigDecimal bestPrice = product.getPrice();

        for (Promotion promotion : promotions) {
            BigDecimal discountedPrice = applyPromotionToPrice(product.getPrice(), promotion);
            if (discountedPrice.compareTo(bestPrice) < 0) {
                bestPrice = discountedPrice;
            }
        }

        return bestPrice;
    }

    public PriceCalculationResult calculateCartPricing(Cart cart) {
        return calculateCartPricing(cart, null);
    }

    public PriceCalculationResult calculateCartPricing(Cart cart, String promotionCode) {
        BigDecimal originalTotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PriceCalculationResult result = new PriceCalculationResult();
        result.setOriginalTotal(originalTotal);
        result.setDiscountAmount(BigDecimal.ZERO);
        result.setFinalTotal(originalTotal);
        result.setAppliedDiscounts(new ArrayList<>());

        // applying automatic discount rules using Drools
        applyAutomaticDiscounts(cart, result);

        // applying promotion code if provided
        if (promotionCode != null && !promotionCode.trim().isEmpty()) {
            applyPromotionCode(cart, promotionCode, result);
        }

        // calculating final total
        result.setFinalTotal(result.getOriginalTotal().subtract(result.getDiscountAmount()));
        if (result.getFinalTotal().compareTo(BigDecimal.ZERO) < 0) {
            result.setFinalTotal(BigDecimal.ZERO);
        }

        return result;
    }

    private void applyAutomaticDiscounts(Cart cart, PriceCalculationResult result) {
        try {
            // converting Cart to CartDto for rules engine
            CartDto cartDto = convertCartToDto(cart);

            // inserting facts into rules engine
            kieSession.insert(cartDto);
            kieSession.insert(result);

            // firing all applicable rules
            kieSession.fireAllRules();

            // clearing session for next use
            kieSession.dispose();
        } catch (Exception e) {
            // fallback to manual rule application if Drools fails
            applyManualDiscountRules(cart, result);
        }
    }

    private void applyManualDiscountRules(Cart cart, PriceCalculationResult result) {
        List<DiscountRule> activeRules = discountRuleRepository.findActiveRules(LocalDateTime.now());

        for (DiscountRule rule : activeRules) {
            if (isRuleApplicable(cart, rule)) {
                BigDecimal discount = calculateRuleDiscount(cart, rule);
                result.setDiscountAmount(result.getDiscountAmount().add(discount));
                result.getAppliedDiscounts().add(rule.getName());
            }
        }
    }

    private boolean isRuleApplicable(Cart cart, DiscountRule rule) {
        switch (rule.getRuleType()) {
            case CART_TOTAL:
                BigDecimal cartTotal = cart.getItems().stream()
                        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return evaluateCondition(cartTotal, rule);

            case QUANTITY_BASED:
                int totalQuantity = cart.getItems().stream()
                        .mapToInt(item -> item.getQuantity())
                        .sum();
                return evaluateCondition(BigDecimal.valueOf(totalQuantity), rule);

            default:
                return false;
        }
    }

    private boolean evaluateCondition(BigDecimal value, DiscountRule rule) {
        BigDecimal conditionValue = new BigDecimal(rule.getConditionValue());

        switch (rule.getConditionOperator()) {
            case GREATER_THAN:
                return value.compareTo(conditionValue) > 0;
            case GREATER_THAN_OR_EQUAL:
                return value.compareTo(conditionValue) >= 0;
            case LESS_THAN:
                return value.compareTo(conditionValue) < 0;
            case LESS_THAN_OR_EQUAL:
                return value.compareTo(conditionValue) <= 0;
            case EQUALS:
                return value.compareTo(conditionValue) == 0;
            default:
                return false;
        }
    }

    private BigDecimal calculateRuleDiscount(Cart cart, DiscountRule rule) {
        BigDecimal cartTotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (rule.getDiscountType() == DiscountRule.DiscountType.PERCENTAGE) {
            return cartTotal.multiply(rule.getDiscountValue().divide(BigDecimal.valueOf(100)));
        } else {
            return rule.getDiscountValue();
        }
    }

    private void applyPromotionCode(Cart cart, String promotionCode, PriceCalculationResult result) {
        Promotion promotion = promotionRepository.findValidPromotionByCode(promotionCode, LocalDateTime.now())
                .orElse(null);

        if (promotion == null) {
            return; // invalid promotion code
        }

        BigDecimal discount = calculatePromotionDiscount(cart, promotion);
        result.setDiscountAmount(result.getDiscountAmount().add(discount));
        result.setAppliedPromotionCode(promotionCode);
        result.getAppliedDiscounts().add(promotion.getName());
    }

    private BigDecimal calculatePromotionDiscount(Cart cart, Promotion promotion) {
        BigDecimal cartTotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (promotion.getMinOrderAmount() != null &&
                cartTotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (promotion.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            discount = cartTotal.multiply(promotion.getDiscountValue().divide(BigDecimal.valueOf(100)));
        } else {
            discount = promotion.getDiscountValue();
        }

        // Apply maximum discount limit if specified
        if (promotion.getMaxDiscountAmount() != null &&
                discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discount = promotion.getMaxDiscountAmount();
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyPromotionToPrice(BigDecimal originalPrice, Promotion promotion) {
        if (promotion.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            BigDecimal discount = originalPrice.multiply(promotion.getDiscountValue().divide(BigDecimal.valueOf(100)));
            return originalPrice.subtract(discount);
        } else {
            return originalPrice.subtract(promotion.getDiscountValue()).max(BigDecimal.ZERO);
        }
    }

    private CartDto convertCartToDto(Cart cart) {
        CartDto dto = new CartDto();
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setSubtotal(subtotal);
        return dto;
    }
}
