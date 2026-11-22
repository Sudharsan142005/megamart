package com.megamart.dto;

import jakarta.validation.constraints.NotBlank;

public class SellerApprovalRequest {

    @NotBlank(message = "Status is required (APPROVED or REJECTED)")
    private String status;

    public SellerApprovalRequest() {
    }

    public SellerApprovalRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
