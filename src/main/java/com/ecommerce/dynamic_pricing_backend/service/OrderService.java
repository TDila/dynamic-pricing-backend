package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.CheckoutRequest;
import com.ecommerce.dynamic_pricing_backend.dto.OrderDto;
import com.ecommerce.dynamic_pricing_backend.dto.OrderItemDto;
import com.ecommerce.dynamic_pricing_backend.entity.*;
import com.ecommerce.dynamic_pricing_backend.repository.CartRepository;
import com.ecommerce.dynamic_pricing_backend.repository.OrderItemRepository;
import com.ecommerce.dynamic_pricing_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final PromotionService promotionService;

    public OrderDto createOrder(Long userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot create order from empty cart");
        }

        var pricingResult = pricingService.calculateCartPricing(cart, request.getPromotionCode());

        Order order = new Order();
        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);
        order.setUser(cart.getUser());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setSubtotal(pricingResult.getOriginalTotal());
        order.setDiscountAmount(pricingResult.getDiscountAmount());
        order.setTotalAmount(pricingResult.getFinalTotal());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getTotalPrice());


            savedOrder.getOrderItems().add(orderItem);
            orderItemRepository.save(orderItem);
            productService.updateStock(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        // tracking promotion usage if applicable
        if (request.getPromotionCode() != null && !request.getPromotionCode().trim().isEmpty()) {
            promotionService.trackPromotionUsage(userId, request.getPromotionCode(), savedOrder.getId());
        }

        // clearing cart after successful order
        cartService.clearCart(userId);

        System.out.println("before process payment");
        // Generate PayHere checkout params
        Map<String, String> payhereParams = paymentService.processPayment(userId, request, pricingResult.getFinalTotal(), orderNumber);
        System.out.println("after process payment");
        // return order + payhere params to frontend
        OrderDto dto = convertToDto(order);
        dto.setPayhereParams(payhereParams);

        return dto;
    }

    public OrderDto getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to order: " + orderId);
        }

        return convertToDto(order);
    }

    public OrderDto getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        return convertToDto(order);
    }

    public OrderDto getOrderByNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to order: " + orderNumber);
        }

        return convertToDto(order);
    }

    public List<OrderDto> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Page<OrderDto> getUserOrdersPaginated(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(this::convertToDto);
    }

    public OrderDto updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        return convertToDto(updatedOrder);
    }

    public List<OrderDto> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public OrderDto cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to order: " + orderId);
        }

        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        if (order.getPaymentId() != null) {
            paymentService.processRefund(order.getPaymentId(), order.getTotalAmount());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order cancelledOrder = orderRepository.save(order);

        return convertToDto(cancelledOrder);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.valueOf((int) (Math.random() * 1000));
        return "ORD-" + timestamp + "-" + randomSuffix;
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setSubtotal(order.getSubtotal());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentId(order.getPaymentId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (order.getOrderItems() != null) {
            List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                    .map(this::convertItemToDto)
                    .collect(Collectors.toList());
            dto.setOrderItems(itemDtos);
        }

        return dto;
    }

    private OrderItemDto convertItemToDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
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
