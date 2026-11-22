package com.megamart.controller;

import com.megamart.dto.*;
import com.megamart.entity.User;
import com.megamart.service.AuthService;
import com.megamart.service.OrderService;
import com.megamart.service.ProductService;
import com.megamart.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final AuthService authService;

    public SellerController(SellerService sellerService,
                            ProductService productService,
                            OrderService orderService,
                            AuthService authService) {
        this.sellerService = sellerService;
        this.productService = productService;
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SellerStatsResponse>> getSellerStats() {
        User seller = authService.getCurrentAuthenticatedUser();
        SellerStatsResponse stats = sellerService.getSellerStats(seller);
        return ResponseEntity.ok(ApiResponse.ok("Seller stats retrieved", stats));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSellerProducts() {
        User seller = authService.getCurrentAuthenticatedUser();
        List<ProductResponse> products = productService.getProductsBySeller(seller.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller products retrieved", products));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        User seller = authService.getCurrentAuthenticatedUser();
        ProductResponse response = productService.createProduct(request, seller);
        return new ResponseEntity<>(ApiResponse.ok("Product created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        User seller = authService.getCurrentAuthenticatedUser();
        ProductResponse response = productService.updateProduct(id, request, seller);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", response));
    }

    @PatchMapping("/products/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer newStock = payload.get("stockQuantity");
        if (newStock == null || newStock < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid non-negative stock quantity required"));
        }
        User seller = authService.getCurrentAuthenticatedUser();
        ProductResponse response = productService.updateStock(id, newStock, seller);
        return ResponseEntity.ok(ApiResponse.ok("Stock updated successfully", response));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        User seller = authService.getCurrentAuthenticatedUser();
        productService.deleteProduct(id, seller);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully"));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderItemResponse>>> getSellerOrders() {
        User seller = authService.getCurrentAuthenticatedUser();
        List<OrderItemResponse> items = orderService.getSellerOrderItems(seller);
        return ResponseEntity.ok(ApiResponse.ok("Seller orders retrieved", items));
    }

    @PatchMapping("/orders/{orderItemId}/status")
    public ResponseEntity<ApiResponse<OrderItemResponse>> updateOrderItemStatus(
            @PathVariable Long orderItemId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        User seller = authService.getCurrentAuthenticatedUser();
        OrderItemResponse updatedItem = orderService.updateSellerOrderItemStatus(orderItemId, request.getStatus(), seller);
        return ResponseEntity.ok(ApiResponse.ok("Order item status updated to " + request.getStatus(), updatedItem));
    }
}
