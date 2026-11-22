package com.megamart.controller;

import com.megamart.dto.ApiResponse;
import com.megamart.dto.OrderResponse;
import com.megamart.dto.PlaceOrderRequest;
import com.megamart.entity.User;
import com.megamart.service.AuthService;
import com.megamart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        User customer = authService.getCurrentAuthenticatedUser();
        OrderResponse response = orderService.placeOrder(customer, request);
        return new ResponseEntity<>(ApiResponse.ok("Order placed successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        User customer = authService.getCurrentAuthenticatedUser();
        List<OrderResponse> orders = orderService.getCustomerOrders(customer);
        return ResponseEntity.ok(ApiResponse.ok("Customer orders retrieved", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        User customer = authService.getCurrentAuthenticatedUser();
        OrderResponse order = orderService.getOrderById(id, customer);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved", order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        User customer = authService.getCurrentAuthenticatedUser();
        OrderResponse response = orderService.cancelOrder(id, customer);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", response));
    }
}
