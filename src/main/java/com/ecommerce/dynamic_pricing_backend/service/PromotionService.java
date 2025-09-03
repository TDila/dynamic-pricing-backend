package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.CreatePromotionRequest;
import com.ecommerce.dynamic_pricing_backend.dto.PromotionDto;
import com.ecommerce.dynamic_pricing_backend.entity.Promotion;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.entity.UserPromotion;
import com.ecommerce.dynamic_pricing_backend.repository.OrderRepository;
import com.ecommerce.dynamic_pricing_backend.repository.PromotionRepository;
import com.ecommerce.dynamic_pricing_backend.repository.UserPromotionRepository;
import com.ecommerce.dynamic_pricing_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Cacheable(value = "promotions")
    public List<PromotionDto> getAllActivePromotions() {
        List<Promotion> promotions = promotionRepository.findActivePromotions(LocalDateTime.now());
        return promotions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public PromotionDto validatePromotionCode(String code, Long userId) {
        Promotion promotion = promotionRepository.findValidPromotionByCode(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired promotion code: " + code));

        // checking if user has already used this promotion
        if (userPromotionRepository.existsByUserIdAndPromotionId(userId, promotion.getId())) {
            throw new RuntimeException("Promotion code has already been used");
        }

        // checking usage limit
        if (promotion.getUsageLimit() != null &&
                promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new RuntimeException("Promotion code usage limit exceeded");
        }

        return convertToDto(promotion);
    }

    @CacheEvict(value = "promotions", allEntries = true)
    public PromotionDto createPromotion(CreatePromotionRequest request) {
        // checking if promotion code already exists
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Promotion code already exists: " + request.getCode());
        }

        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setCode(request.getCode().toUpperCase());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsedCount(0);
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setCategory(request.getCategory());
        promotion.setBrand(request.getBrand());
        promotion.setActive(true);
        promotion.setCreatedAt(LocalDateTime.now());

        Promotion savedPromotion = promotionRepository.save(promotion);
        return convertToDto(savedPromotion);
    }

    @CacheEvict(value = "promotions", allEntries = true)
    public PromotionDto updatePromotion(Long id, CreatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setCategory(request.getCategory());
        promotion.setBrand(request.getBrand());
        promotion.setUpdatedAt(LocalDateTime.now());

        Promotion updatedPromotion = promotionRepository.save(promotion);
        return convertToDto(updatedPromotion);
    }

    @CacheEvict(value = "promotions", allEntries = true)
    public void deactivatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));

        promotion.setActive(false);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    public void trackPromotionUsage(Long userId, String promotionCode, Long orderId) {
        System.out.println("searching for user");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        System.out.println("user found: "+user.getId());

        Promotion promotion = promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new RuntimeException("Promotion not found with code: " + promotionCode));
        System.out.println("after searching user promotion: "+promotion.getCode());
        // creating usage record
        UserPromotion userPromotion = new UserPromotion();
        userPromotion.setUser(user);
        userPromotion.setPromotion(promotion);
        userPromotion.setOrder(orderRepository.findById(orderId).orElse(null));
        userPromotion.setUsedAt(LocalDateTime.now());
        System.out.println("user promotion: "+userPromotion.getPromotion().getName());
        userPromotionRepository.save(userPromotion);

        // incrementing usage count
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);
    }

    public List<PromotionDto> getUserPromotionHistory(Long userId) {
        List<UserPromotion> userPromotions = userPromotionRepository.findByUserId(userId);
        return userPromotions.stream()
                .map(up -> convertToDto(up.getPromotion()))
                .collect(Collectors.toList());
    }

    public List<PromotionDto> getPromotionsByCategory(String category) {
        List<Promotion> promotions = promotionRepository.findActivePromotionsByCategory(category, LocalDateTime.now());
        return promotions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PromotionDto> getPromotionsByBrand(String brand) {
        List<Promotion> promotions = promotionRepository.findActivePromotionsByBrand(brand, LocalDateTime.now());
        return promotions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private PromotionDto convertToDto(Promotion promotion) {
        PromotionDto dto = new PromotionDto();
        dto.setId(promotion.getId());
        dto.setName(promotion.getName());
        dto.setDescription(promotion.getDescription());
        dto.setCode(promotion.getCode());
        dto.setDiscountType(promotion.getDiscountType());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setMinOrderAmount(promotion.getMinOrderAmount());
        dto.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        dto.setUsageLimit(promotion.getUsageLimit());
        dto.setUsedCount(promotion.getUsedCount());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setActive(promotion.getActive());
        dto.setCategory(promotion.getCategory());
        dto.setBrand(promotion.getBrand());
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setUpdatedAt(promotion.getUpdatedAt());
        return dto;
    }
}
