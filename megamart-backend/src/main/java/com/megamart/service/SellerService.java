package com.megamart.service;

import com.megamart.dto.SellerStatsResponse;
import com.megamart.entity.OrderItem;
import com.megamart.entity.User;
import com.megamart.repository.OrderItemRepository;
import com.megamart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SellerService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public SellerService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public SellerStatsResponse getSellerStats(User seller) {
        long totalProducts = productRepository.countBySellerId(seller.getId());
        List<OrderItem> sellerItems = orderItemRepository.findBySellerIdOrderByOrderCreatedAtDesc(seller.getId());

        long totalOrders = sellerItems.stream()
                .map(item -> item.getOrder().getId())
                .distinct()
                .count();

        long totalUnitsSold = sellerItems.stream()
                .filter(item -> !"CANCELLED".equalsIgnoreCase(item.getStatus()))
                .mapToLong(OrderItem::getQuantity)
                .sum();

        BigDecimal totalRevenue = sellerItems.stream()
                .filter(item -> !"CANCELLED".equalsIgnoreCase(item.getStatus()))
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerStatsResponse(totalProducts, totalOrders, totalUnitsSold, totalRevenue);
    }
}
