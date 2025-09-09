package com.airticket.dto;

public class AdminApprovalResponse {
    private String action; // "APPROVE" or "REJECT"
    private String rejectionReason; // Required if action is "REJECT"

    public AdminApprovalResponse() {}

    public AdminApprovalResponse(String action, String rejectionReason) {
        this.action = action;
        this.rejectionReason = rejectionReason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}