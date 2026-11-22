package com.megamart.dto;

import java.math.BigDecimal;

public class AdminStatsResponse {
    private long totalCustomers;
    private long totalSellers;
    private long pendingSellers;
    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalRevenue;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalCustomers, long totalSellers, long pendingSellers, long totalProducts, long totalOrders, BigDecimal totalRevenue) {
        this.totalCustomers = totalCustomers;
        this.totalSellers = totalSellers;
        this.pendingSellers = pendingSellers;
        this.totalProducts = totalProducts;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalSellers() {
        return totalSellers;
    }

    public void setTotalSellers(long totalSellers) {
        this.totalSellers = totalSellers;
    }

    public long getPendingSellers() {
        return pendingSellers;
    }

    public void setPendingSellers(long pendingSellers) {
        this.pendingSellers = pendingSellers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
