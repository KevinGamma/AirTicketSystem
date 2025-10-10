package com.airticket.dto;

public class AdminApprovalResponse {
    private String action; 
    private String rejectionReason; 

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

}