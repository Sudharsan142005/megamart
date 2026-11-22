package com.megamart.service;

import com.megamart.dto.AddToCartRequest;
import com.megamart.dto.CartItemResponse;
import com.megamart.dto.CartResponse;
import com.megamart.entity.Cart;
import com.megamart.entity.CartItem;
import com.megamart.entity.Product;
import com.megamart.entity.User;
import com.megamart.exception.BadRequestException;
import com.megamart.exception.ForbiddenException;
import com.megamart.exception.InsufficientStockException;
import com.megamart.exception.ResourceNotFoundException;
import com.megamart.repository.CartItemRepository;
import com.megamart.repository.CartRepository;
import com.megamart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Cart getOrCreateCart(User customer) {
        return cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart(customer);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartResponse getCartForCustomer(User customer) {
        Cart cart = getOrCreateCart(customer);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(User customer, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("This product is currently inactive and cannot be added to cart.");
        }

        if (product.getStockQuantity() <= 0) {
            throw new InsufficientStockException("Product '" + product.getName() + "' is out of stock.");
        }

        Cart cart = getOrCreateCart(customer);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (newQuantity > product.getStockQuantity()) {
                throw new InsufficientStockException("Cannot add more of '" + product.getName() + "'. Available stock: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(product.getPrice());
            cartItemRepository.save(existingItem);
        } else {
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new InsufficientStockException("Requested quantity exceeds available stock (" + product.getStockQuantity() + ")");
            }
            CartItem newItem = new CartItem(cart, product, request.getQuantity(), product.getPrice());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.save(cart);
        return mapToCartResponse(updatedCart);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(User customer, Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return removeCartItem(customer, itemId);
        }

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You can only modify your own cart.");
        }

        Product product = cartItem.getProduct();
        if (quantity > product.getStockQuantity()) {
            throw new InsufficientStockException("Requested quantity exceeds available stock (" + product.getStockQuantity() + ")");
        }

        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(product.getPrice());
        cartItemRepository.save(cartItem);

        return mapToCartResponse(cartItem.getCart());
    }

    @Transactional
    public CartResponse removeCartItem(User customer, Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You can only modify your own cart.");
        }

        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToCartResponse(cart);
    }

    @Transactional
    public void clearCart(User customer) {
        Cart cart = getOrCreateCart(customer);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse ir = new CartItemResponse();
                    ir.setId(item.getId());
                    ir.setProductId(item.getProduct().getId());
                    ir.setProductName(item.getProduct().getName());
                    ir.setProductImageUrl(item.getProduct().getImageUrl());
                    ir.setUnitPrice(item.getUnitPrice());
                    ir.setQuantity(item.getQuantity());
                    ir.setSubtotal(item.getSubtotal());
                    ir.setAvailableStock(item.getProduct().getStockQuantity());
                    ir.setInStock(item.getProduct().getStockQuantity() >= item.getQuantity());
                    return ir;
                })
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        response.setTotalQuantity(cart.getTotalQuantity());
        response.setTotalPrice(cart.getTotalPrice());
        return response;
    }
}
