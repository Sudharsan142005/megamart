package com.megamart.controller;

import com.megamart.dto.*;
import com.megamart.entity.Category;
import com.megamart.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;

    public AdminController(AdminService adminService,
                           ProductService productService,
                           OrderService orderService,
                           CategoryService categoryService) {
        this.adminService = adminService;
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getDashboardStats() {
        AdminStatsResponse stats = adminService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.ok("Admin stats retrieved", stats));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllCustomers() {
        List<UserResponse> customers = adminService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.ok("Customers retrieved", customers));
    }

    @GetMapping("/sellers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllSellers() {
        List<UserResponse> sellers = adminService.getAllSellers();
        return ResponseEntity.ok(ApiResponse.ok("Sellers retrieved", sellers));
    }

    @GetMapping("/sellers/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingSellers() {
        List<UserResponse> pending = adminService.getPendingSellers();
        return ResponseEntity.ok(ApiResponse.ok("Pending sellers retrieved", pending));
    }

    @PostMapping("/sellers/{id}/approval")
    public ResponseEntity<ApiResponse<UserResponse>> approveOrRejectSeller(
            @PathVariable Long id,
            @Valid @RequestBody SellerApprovalRequest request) {
        UserResponse response = adminService.approveOrRejectSeller(id, request.getStatus());
        String msg = "Seller status updated to " + request.getStatus();
        return ResponseEntity.ok(ApiResponse.ok(msg, response));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody UserStatusUpdateRequest request) {
        UserResponse response = adminService.toggleUserStatus(id, request.isEnabled());
        String msg = "User account has been " + (request.isEnabled() ? "enabled" : "disabled");
        return ResponseEntity.ok(ApiResponse.ok(msg, response));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(ApiResponse.ok("All products retrieved", products));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(ApiResponse.ok("All orders retrieved", orders));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateOrderStatusByAdmin(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated to " + request.getStatus(), response));
    }

    // Category Management
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return new ResponseEntity<>(ApiResponse.ok("Category created successfully", category), HttpStatus.CREATED);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully"));
    }
}
