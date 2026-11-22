package com.megamart.service;

import com.megamart.dto.ProductRequest;
import com.megamart.dto.ProductResponse;
import com.megamart.entity.Category;
import com.megamart.entity.Product;
import com.megamart.entity.User;
import com.megamart.exception.ForbiddenException;
import com.megamart.exception.ResourceNotFoundException;
import com.megamart.repository.CategoryRepository;
import com.megamart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword, Long categoryId) {
        List<Product> products;
        if (categoryId != null && keyword != null && !keyword.isBlank()) {
            products = productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndActiveTrue(categoryId, keyword.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        } else if (keyword != null && !keyword.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
        } else {
            products = productRepository.findByActiveTrue();
        }

        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, User seller) {
        if (!seller.isApproved()) {
            throw new ForbiddenException("Your seller account is awaiting administrator approval before you can add products.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStockQuantity(),
                request.getImageUrl() != null && !request.getImageUrl().isBlank()
                        ? request.getImageUrl()
                        : "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80",
                category,
                seller
        );
        product.setActive(request.isActive());

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, User seller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You can only modify products from your own catalog.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl());
        }
        product.setCategory(category);
        product.setActive(request.isActive());

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Transactional
    public ProductResponse updateStock(Long productId, Integer newStock, User seller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You can only modify products from your own catalog.");
        }

        product.setStockQuantity(newStock);
        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId, User seller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You can only delete products from your own catalog.");
        }

        productRepository.delete(product);
    }

    public ProductResponse mapToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setImageUrl(product.getImageUrl());
        response.setActive(product.isActive());
        response.setCategoryId(product.getCategory().getId());
        response.setCategoryName(product.getCategory().getName());
        response.setSellerId(product.getSeller().getId());
        response.setSellerName(product.getSeller().getFullName());
        response.setSellerStoreName(product.getSeller().getStoreName());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
