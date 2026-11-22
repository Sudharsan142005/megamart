package com.megamart.repository;

import com.megamart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findBySellerIdOrderByOrderCreatedAtDesc(Long sellerId);
    List<OrderItem> findByOrderId(Long orderId);
}
