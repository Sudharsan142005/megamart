package com.megamart.dto;

import jakarta.validation.constraints.NotBlank;

public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    private String paymentMethod = "CASH_ON_DELIVERY";

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String shippingAddress, String contactPhone, String paymentMethod) {
        this.shippingAddress = shippingAddress;
        this.contactPhone = contactPhone;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH_ON_DELIVERY";
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
