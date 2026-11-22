package com.megamart.service;

import com.megamart.dto.OrderItemResponse;
import com.megamart.dto.OrderResponse;
import com.megamart.dto.PlaceOrderRequest;
import com.megamart.entity.*;
import com.megamart.exception.BadRequestException;
import com.megamart.exception.ForbiddenException;
import com.megamart.exception.InsufficientStockException;
import com.megamart.exception.ResourceNotFoundException;
import com.megamart.repository.CartRepository;
import com.megamart.repository.OrderItemRepository;
import com.megamart.repository.OrderRepository;
import com.megamart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse placeOrder(User customer, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with an empty cart");
        }

        // Validate stock for all items before making any modifications
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer available.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product '" + product.getName() +
                        "'. Requested: " + cartItem.getQuantity() + ", Available: " + product.getStockQuantity());
            }
        }

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setShippingAddress(request.getShippingAddress());
        order.setContactPhone(request.getContactPhone());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH_ON_DELIVERY");
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    product.getSeller(),
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice(),
                    cartItem.getSubtotal()
            );
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(cartItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(User customer) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        boolean isOwner = order.getCustomer().getId().equals(user.getId());
        boolean isSellerOfItem = order.getOrderItems().stream().anyMatch(item -> item.getSeller().getId().equals(user.getId()));

        if (!isAdmin && !isOwner && !isSellerOfItem) {
            throw new ForbiddenException("You do not have permission to view this order.");
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, User customer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You can only cancel your own orders.");
        }

        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus()) && !"CONFIRMED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Order cannot be cancelled as it is already " + order.getOrderStatus());
        }

        order.setOrderStatus("CANCELLED");

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
            item.setStatus("CANCELLED");
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getSellerOrderItems(User seller) {
        return orderItemRepository.findBySellerIdOrderByOrderCreatedAtDesc(seller.getId()).stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderItemResponse updateSellerOrderItemStatus(Long orderItemId, String newStatus, User seller) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + orderItemId));

        if (!item.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You can only update status for your own product orders.");
        }

        String currentStatus = item.getStatus();
        item.setStatus(newStatus.toUpperCase());

        // If cancelled by seller, restore product stock
        if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(currentStatus)) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        OrderItem updatedItem = orderItemRepository.save(item);

        // Check if all items in the parent order have same status
        Order parentOrder = item.getOrder();
        boolean allSame = parentOrder.getOrderItems().stream().allMatch(i -> i.getStatus().equalsIgnoreCase(newStatus));
        if (allSame) {
            parentOrder.setOrderStatus(newStatus.toUpperCase());
            orderRepository.save(parentOrder);
        }

        return mapToOrderItemResponse(updatedItem);
    }

    @Transactional
    public OrderResponse updateOrderStatusByAdmin(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        String oldStatus = order.getOrderStatus();
        String upperStatus = newStatus.toUpperCase();
        order.setOrderStatus(upperStatus);

        if ("CANCELLED".equalsIgnoreCase(upperStatus) && !"CANCELLED".equalsIgnoreCase(oldStatus)) {
            // Restore stock
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                item.setStatus("CANCELLED");
            }
        } else {
            for (OrderItem item : order.getOrderItems()) {
                item.setStatus(upperStatus);
            }
        }

        if ("DELIVERED".equalsIgnoreCase(upperStatus)) {
            order.setPaymentStatus("PAID");
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    public OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getCustomer().getId());
        response.setCustomerName(order.getCustomer().getFullName());
        response.setCustomerEmail(order.getCustomer().getEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingAddress(order.getShippingAddress());
        response.setContactPhone(order.getContactPhone());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        response.setOrderItems(itemResponses);

        return response;
    }

    public OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setProductImageUrl(item.getProduct().getImageUrl());
        response.setSellerId(item.getSeller().getId());
        response.setSellerName(item.getSeller().getFullName());
        response.setSellerStoreName(item.getSeller().getStoreName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(item.getSubtotal());
        response.setStatus(item.getStatus());
        return response;
    }
}
