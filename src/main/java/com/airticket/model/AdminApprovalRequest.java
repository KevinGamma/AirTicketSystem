package com.airticket.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class AdminApprovalRequest {
    private Long id;
    private Long ticketId;
    private Long userId;
    private String requestType; // "REFUND" or "RESCHEDULE"
    private String status; // "PENDING", "APPROVED", "REJECTED", "AWAITING_PAYMENT", "PAYMENT_COMPLETED"
    private String reason;
    private BigDecimal serviceFee;
    private Long newFlightId; // Only for reschedule requests
    
    // Payment-related fields for reschedule operations
    private BigDecimal totalAmount; // Total additional amount to be paid or refunded
    private BigDecimal refundAmount; // Amount to be refunded (if negative difference)
    private String paymentStatus; // "NOT_REQUIRED", "PAYMENT_REQUIRED", "REFUND_REQUIRED", "COMPLETED"
    private String paymentNumber; // Payment transaction number for additional payment
    private String refundNumber; // Refund transaction number
    private Long approvedByAdminId;
    private String rejectionReason;
    private LocalDateTime requestTime;
    private LocalDateTime processedTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Join fields for display
    private Ticket ticket;
    private User user;
    private User approvedByAdmin;
    private Flight newFlight; // Only for reschedule requests

    public AdminApprovalRequest() {}

    public AdminApprovalRequest(Long ticketId, Long userId, String requestType, String reason, BigDecimal serviceFee, Long newFlightId) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.requestType = requestType;
        this.reason = reason;
        this.serviceFee = serviceFee;
        this.newFlightId = newFlightId;
        this.status = "PENDING";
        this.requestTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public Long getNewFlightId() {
        return newFlightId;
    }

    public void setNewFlightId(Long newFlightId) {
        this.newFlightId = newFlightId;
    }

    public Long getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public void setApprovedByAdminId(Long approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public LocalDateTime getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(LocalDateTime processedTime) {
        this.processedTime = processedTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getApprovedByAdmin() {
        return approvedByAdmin;
    }

    public void setApprovedByAdmin(User approvedByAdmin) {
        this.approvedByAdmin = approvedByAdmin;
    }

    public Flight getNewFlight() {
        return newFlight;
    }

    public void setNewFlight(Flight newFlight) {
        this.newFlight = newFlight;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public String getRefundNumber() {
        return refundNumber;
    }

    public void setRefundNumber(String refundNumber) {
        this.refundNumber = refundNumber;
    }

    // Alias methods for backward compatibility
    public BigDecimal getAdditionalFee() {
        return totalAmount;
    }

    public void setAdditionalFee(BigDecimal additionalFee) {
        this.totalAmount = additionalFee;
    }
}