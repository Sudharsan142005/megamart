package com.megamart.dto;

import java.math.BigDecimal;

public class SellerStatsResponse {
    private long totalProducts;
    private long totalOrders;
    private long totalUnitsSold;
    private BigDecimal totalRevenue;

    public SellerStatsResponse() {
    }

    public SellerStatsResponse(long totalProducts, long totalOrders, long totalUnitsSold, BigDecimal totalRevenue) {
        this.totalProducts = totalProducts;
        this.totalOrders = totalOrders;
        this.totalUnitsSold = totalUnitsSold;
        this.totalRevenue = totalRevenue;
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

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(long totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
