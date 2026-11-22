package com.megamart.controller;

import com.megamart.dto.AddToCartRequest;
import com.megamart.dto.ApiResponse;
import com.megamart.dto.CartResponse;
import com.megamart.dto.UpdateCartItemRequest;
import com.megamart.entity.User;
import com.megamart.service.AuthService;
import com.megamart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final AuthService authService;

    public CartController(CartService cartService, AuthService authService) {
        this.cartService = cartService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        User customer = authService.getCurrentAuthenticatedUser();
        CartResponse cart = cartService.getCartForCustomer(customer);
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        User customer = authService.getCurrentAuthenticatedUser();
        CartResponse cart = cartService.addToCart(customer, request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        User customer = authService.getCurrentAuthenticatedUser();
        CartResponse cart = cartService.updateCartItemQuantity(customer, itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok("Cart quantity updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long itemId) {
        User customer = authService.getCurrentAuthenticatedUser();
        CartResponse cart = cartService.removeCartItem(customer, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        User customer = authService.getCurrentAuthenticatedUser();
        cartService.clearCart(customer);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared successfully"));
    }
}
