package com.ecommerce.dynamic_pricing_backend.controller;

import com.ecommerce.dynamic_pricing_backend.dto.CreatePromotionRequest;
import com.ecommerce.dynamic_pricing_backend.dto.PromotionDto;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.service.PromotionService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // ==== PUBLIC ENDPOINTS (No Authentication Required) ====

    @GetMapping
    public ResponseEntity<List<PromotionDto>> getAllActivePromotions() {
        List<PromotionDto> promotions = promotionService.getAllActivePromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<PromotionDto>> getPromotionsByCategory(
            @Parameter(description = "Product category") @PathVariable String category) {
        List<PromotionDto> promotions = promotionService.getPromotionsByCategory(category);
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<PromotionDto>> getPromotionsByBrand(
            @Parameter(description = "Product brand") @PathVariable String brand) {
        List<PromotionDto> promotions = promotionService.getPromotionsByBrand(brand);
        return ResponseEntity.ok(promotions);
    }

    // ==== CUSTOMER ENDPOINTS (Customer Authentication Required) ====

    @PostMapping("/validate/{code}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PromotionDto> validatePromotionCode(@PathVariable String code, @AuthenticationPrincipal User user) {
        try {
            PromotionDto promotion = promotionService.validatePromotionCode(code.toUpperCase(), user.getId());
            return ResponseEntity.ok(promotion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PromotionDto>> getUserPromotionHistory(
            @AuthenticationPrincipal User user) {
        List<PromotionDto> promotions = promotionService.getUserPromotionHistory(user.getId());
        return ResponseEntity.ok(promotions);
    }

    // ==== ADMIN ENDPOINTS (Admin Authentication Required) ====

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionDto> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {
        try {
            PromotionDto promotion = promotionService.createPromotion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(promotion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionDto> updatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id,
            @Valid @RequestBody CreatePromotionRequest request) {
        try {
            PromotionDto promotion = promotionService.updatePromotion(id, request);
            return ResponseEntity.ok(promotion);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        try {
            promotionService.deactivatePromotion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/admin/user/{userId}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromotionDto>> getUserPromotionHistoryByAdmin(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<PromotionDto> promotions = promotionService.getUserPromotionHistory(userId);
        return ResponseEntity.ok(promotions);
    }
}
