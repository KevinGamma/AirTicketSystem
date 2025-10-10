package com.airticket.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long ticketId;                  
    private BigDecimal amount;              
    private String paymentMethod;           
    private String returnUrl;               
    private boolean useSandbox;             
    
    
    private String sandboxBuyerAccount;     
    private String testScenario;            
    
    public PaymentRequest() {}
    
    public PaymentRequest(Long ticketId, BigDecimal amount, String paymentMethod) {
        this.ticketId = ticketId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.useSandbox = true; 
    }
    
    
    public Long getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
    public boolean isUseSandbox() {
        return useSandbox;
    }
    
    public void setUseSandbox(boolean useSandbox) {
        this.useSandbox = useSandbox;
    }
    
    public String getSandboxBuyerAccount() {
        return sandboxBuyerAccount;
    }
    
    public void setSandboxBuyerAccount(String sandboxBuyerAccount) {
        this.sandboxBuyerAccount = sandboxBuyerAccount;
    }
    
    public String getTestScenario() {
        return testScenario;
    }
    
    public void setTestScenario(String testScenario) {
        this.testScenario = testScenario;
    }
}