package com.airticket.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Instant;





public class Payment {
    private Long id;
    private Long ticketId;
    private String paymentNumber;           
    private String alipayTradeNo;          
    private BigDecimal amount;              
    private String currency;                
    private String paymentMethod;           
    private String status;                  
    private String subject;                 
    private String body;                    
    private String buyerLogonId;           
    private String buyerUserId;            
    private Instant paymentTime;      
    private Instant createdAt;        
    private Instant updatedAt;        
    
    
    private boolean sandboxMode;            
    private String sandboxBuyerAccount;     
    
    
    private Ticket ticket;
    
    public Payment() {}
    
    public Payment(Long ticketId, String paymentNumber, BigDecimal amount, 
                   String subject, String body, boolean sandboxMode) {
        this.ticketId = ticketId;
        this.paymentNumber = paymentNumber;
        this.amount = amount;
        this.currency = "CNY";
        this.paymentMethod = "ALIPAY";
        this.status = "PENDING";
        this.subject = subject;
        this.body = body;
        this.sandboxMode = sandboxMode;
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
    
    public String getPaymentNumber() {
        return paymentNumber;
    }
    
    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }
    
    public String getAlipayTradeNo() {
        return alipayTradeNo;
    }
    
    public void setAlipayTradeNo(String alipayTradeNo) {
        this.alipayTradeNo = alipayTradeNo;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public String getBuyerLogonId() {
        return buyerLogonId;
    }
    
    public void setBuyerLogonId(String buyerLogonId) {
        this.buyerLogonId = buyerLogonId;
    }
    
    public String getBuyerUserId() {
        return buyerUserId;
    }
    
    public void setBuyerUserId(String buyerUserId) {
        this.buyerUserId = buyerUserId;
    }
    
    public Instant getPaymentTime() {
        return paymentTime;
    }
    
    public void setPaymentTime(Instant paymentTime) {
        this.paymentTime = paymentTime;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isSandboxMode() {
        return sandboxMode;
    }
    
    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }
    
    public String getSandboxBuyerAccount() {
        return sandboxBuyerAccount;
    }
    
    public void setSandboxBuyerAccount(String sandboxBuyerAccount) {
        this.sandboxBuyerAccount = sandboxBuyerAccount;
    }
    
    public Ticket getTicket() {
        return ticket;
    }
    
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}