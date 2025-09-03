package com.ecommerce.dynamic_pricing_backend.controller;

import com.ecommerce.dynamic_pricing_backend.dto.AddToCartRequest;
import com.ecommerce.dynamic_pricing_backend.dto.CartDto;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.service.CartService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal User user) {
        try {
            CartDto cart = cartService.getCartByUserId(user.getId());
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItemToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest request) {
        try {
            CartDto cart = cartService.addItemToCart(user.getId(), request);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("not available") ||
                    e.getMessage().contains("Insufficient stock")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "New quantity (0 or negative removes item)") @RequestParam Integer quantity) {
        try {
            CartDto cart = cartService.updateCartItemQuantity(user.getId(), productId, quantity);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("Insufficient stock")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> removeItemFromCart(@AuthenticationPrincipal User user,@PathVariable Long productId) {
        try {
            CartDto cart = cartService.removeItemFromCart(user.getId(), productId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        try {
            cartService.clearCart(user.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
