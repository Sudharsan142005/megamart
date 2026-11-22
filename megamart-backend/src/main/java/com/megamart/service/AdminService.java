package com.megamart.service;

import com.megamart.dto.AdminStatsResponse;
import com.megamart.dto.UserResponse;
import com.megamart.entity.ERole;
import com.megamart.entity.Order;
import com.megamart.entity.User;
import com.megamart.exception.BadRequestException;
import com.megamart.exception.ResourceNotFoundException;
import com.megamart.repository.OrderRepository;
import com.megamart.repository.ProductRepository;
import com.megamart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;

    public AdminService(UserRepository userRepository,
                        ProductRepository productRepository,
                        OrderRepository orderRepository,
                        AuthService authService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        long totalCustomers = userRepository.countByRolesName(ERole.ROLE_CUSTOMER);
        long totalSellers = userRepository.countByRolesName(ERole.ROLE_SELLER);
        long pendingSellers = userRepository.countByRolesNameAndApprovalStatus(ERole.ROLE_SELLER, "PENDING");
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(order -> !"CANCELLED".equalsIgnoreCase(order.getOrderStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminStatsResponse(totalCustomers, totalSellers, pendingSellers, totalProducts, totalOrders, totalRevenue);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllCustomers() {
        return userRepository.findByRolesName(ERole.ROLE_CUSTOMER).stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllSellers() {
        return userRepository.findByRolesName(ERole.ROLE_SELLER).stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getPendingSellers() {
        return userRepository.findByRolesNameAndApprovalStatus(ERole.ROLE_SELLER, "PENDING").stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse approveOrRejectSeller(Long sellerId, String status) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        boolean isSeller = seller.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_SELLER);
        if (!isSeller) {
            throw new BadRequestException("User is not registered as a seller");
        }

        String upperStatus = status.toUpperCase();
        if ("APPROVED".equals(upperStatus)) {
            seller.setApproved(true);
            seller.setApprovalStatus("APPROVED");
        } else if ("REJECTED".equals(upperStatus)) {
            seller.setApproved(false);
            seller.setApprovalStatus("REJECTED");
        } else {
            throw new BadRequestException("Status must be either APPROVED or REJECTED");
        }

        User updatedUser = userRepository.save(seller);
        return authService.mapToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        if (isAdmin) {
            throw new BadRequestException("Cannot disable administrator accounts");
        }

        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);
        return authService.mapToUserResponse(updatedUser);
    }
}
