package com.airticket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payment Response DTO
 * 支付响应数据传输对象
 */
public class PaymentResponse {
    private boolean success;                // 是否成功
    private String message;                 // 响应消息
    private String paymentNumber;           // 支付单号
    private String alipayTradeNo;          // 支付宝交易号
    private BigDecimal amount;              // 支付金额
    private String status;                  // 支付状态
    private String paymentUrl;              // 支付页面URL (用于网页支付)
    private String qrCode;                  // 二维码内容 (用于扫码支付)
    private LocalDateTime paymentTime;      // 支付时间
    
    // 沙箱环境提示信息
    private boolean sandboxMode;            // 是否沙箱模式
    private String sandboxTips;             // 沙箱测试提示
    private SandboxTestInfo sandboxTestInfo; // 沙箱测试信息
    
    // 退款相关信息
    private Map<String, Object> refundDetails; // 退款详情
    
    public static class SandboxTestInfo {
        private String buyerAccount;        // 测试买家账号
        private String payPassword;        // 测试支付密码
        private String testInstructions;   // 测试说明
        
        public SandboxTestInfo() {}
        
        public SandboxTestInfo(String buyerAccount, String payPassword, String testInstructions) {
            this.buyerAccount = buyerAccount;
            this.payPassword = payPassword;
            this.testInstructions = testInstructions;
        }
        
        // Getters and Setters
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
    
    // Getters and Setters
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