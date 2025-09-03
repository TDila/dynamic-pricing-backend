package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.AddToCartRequest;
import com.ecommerce.dynamic_pricing_backend.dto.CartDto;
import com.ecommerce.dynamic_pricing_backend.dto.CartItemDto;
import com.ecommerce.dynamic_pricing_backend.entity.Cart;
import com.ecommerce.dynamic_pricing_backend.entity.CartItem;
import com.ecommerce.dynamic_pricing_backend.entity.Product;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.repository.CartItemRepository;
import com.ecommerce.dynamic_pricing_backend.repository.CartRepository;
import com.ecommerce.dynamic_pricing_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;

    public CartDto getCartByUserId(Long userId){
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        return convertToDto(cart);
    }

    public CartDto addItemToCart(Long userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        if (!product.getActive()) {
            throw new RuntimeException("Product is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        // Check if item already exists in cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            // Update existing item
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
            cartItemRepository.save(existingItem);
        } else {
            // Create new item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setUnitPrice(product.getPrice());
            cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            cart.getItems().add(cartItem);
            cartItemRepository.save(cartItem);
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return convertToDto(savedCart);
    }


    public CartDto updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
            }

            cartItem.setQuantity(quantity);
            cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            cartItemRepository.save(cartItem);
        }

        updateCartTotals(cart);
        return convertToDto(cart);
    }

    @Transactional
    public CartDto removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found for product: " + productId));

        cart.getItems().remove(itemToRemove);

        cartItemRepository.delete(itemToRemove);
        updateCartTotals(cart);
        return convertToDto(cart);
    }


    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    public void createCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCreatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(subtotal);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        System.out.println("Inside updateCartTotals: fresh cart total = " + cart.getTotalAmount());
    }

    private CartDto convertToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDto> itemDTOs = cart.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        // calculating pricing with discounts
        var pricingResult = pricingService.calculateCartPricing(cart);
        dto.setSubtotal(pricingResult.getOriginalTotal());
        dto.setDiscountAmount(pricingResult.getDiscountAmount());
        dto.setTotalAmount(pricingResult.getFinalTotal());
        dto.setAppliedPromotionCode(pricingResult.getAppliedPromotionCode());

        System.out.println("dto: "+dto);

        return dto;
    }

    private CartItemDto convertItemToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }
}
