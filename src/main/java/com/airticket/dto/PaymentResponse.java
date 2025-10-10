package com.airticket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;





public class PaymentResponse {
    private boolean success;                
    private String message;                 
    private String paymentNumber;           
    private String alipayTradeNo;          
    private BigDecimal amount;              
    private String status;                  
    private String paymentUrl;              
    private String qrCode;                  
    private LocalDateTime paymentTime;      
    
    
    private boolean sandboxMode;            
    private String sandboxTips;             
    private SandboxTestInfo sandboxTestInfo; 
    
    
    private Map<String, Object> refundDetails; 
    
    public static class SandboxTestInfo {
        private String buyerAccount;        
        private String payPassword;        
        private String testInstructions;   
        
        public SandboxTestInfo() {}
        
        public SandboxTestInfo(String buyerAccount, String payPassword, String testInstructions) {
            this.buyerAccount = buyerAccount;
            this.payPassword = payPassword;
            this.testInstructions = testInstructions;
        }
        
        
        public String getBuyerAccount() { return buyerAccount; }
        public void setBuyerAccount(String buyerAccount) { this.buyerAccount = buyerAccount; }
        
        public String getPayPassword() { return payPassword; }
        public void setPayPassword(String payPassword) { this.payPassword = payPassword; }
        
        public String getTestInstructions() { return testInstructions; }
        public void setTestInstructions(String testInstructions) { this.testInstructions = testInstructions; }
    }
    
    public PaymentResponse() {}
    
    public PaymentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public static PaymentResponse success(String paymentNumber, String paymentUrl) {
        PaymentResponse response = new PaymentResponse(true, "支付订单创建成功");
        response.setPaymentNumber(paymentNumber);
        response.setPaymentUrl(paymentUrl);
        return response;
    }
    
    public static PaymentResponse error(String message) {
        return new PaymentResponse(false, message);
    }
    
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPaymentUrl() {
        return paymentUrl;
    }
    
    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }
    
    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }
    
    public boolean isSandboxMode() {
        return sandboxMode;
    }
    
    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }
    
    public String getSandboxTips() {
        return sandboxTips;
    }
    
    public void setSandboxTips(String sandboxTips) {
        this.sandboxTips = sandboxTips;
    }
    
    public SandboxTestInfo getSandboxTestInfo() {
        return sandboxTestInfo;
    }
    
    public void setSandboxTestInfo(SandboxTestInfo sandboxTestInfo) {
        this.sandboxTestInfo = sandboxTestInfo;
    }
    
    public Map<String, Object> getRefundDetails() {
        return refundDetails;
    }
    
    public void setRefundDetails(Map<String, Object> refundDetails) {
        this.refundDetails = refundDetails;
    }
}