package com.airticket.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long ticketId;                  // 票据ID
    private BigDecimal amount;              // 支付金额
    private String paymentMethod;           // 支付方式: ALIPAY, WECHAT, CARD
    private String returnUrl;               // 支付成功返回地址
    private boolean useSandbox;             // 是否使用沙箱环境
    
    // 沙箱测试相关字段
    private String sandboxBuyerAccount;     // 沙箱买家账号 (可选，用于指定测试账号)
    private String testScenario;            // 测试场景: SUCCESS, FAIL, TIMEOUT等
    
    public PaymentRequest() {}
    
    public PaymentRequest(Long ticketId, BigDecimal amount, String paymentMethod) {
        this.ticketId = ticketId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.useSandbox = true; // 默认使用沙箱
    }
    
    // Getters and Setters
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