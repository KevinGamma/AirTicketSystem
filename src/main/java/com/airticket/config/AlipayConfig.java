package com.airticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Alipay Sandbox Configuration
 * 支付宝沙箱环境配置
 */
@Configuration
@ConfigurationProperties(prefix = "alipay.sandbox")
public class AlipayConfig {
    
    // 沙箱环境网关地址
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    
    // 应用ID (你的真实沙箱应用ID)
    private String appId = "9021000151637825";
    
    // 应用私钥 (需要从支付宝开发平台配置)
    private String appPrivateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCn6if7ZYUiriRJEsSJbP8ZqbLMxWhkOoA2iC2XDfFTX4W4CMEGPtuMFj/ZaDE3+B1N5VKPIdEiW9JloQ5evpdX9fTGbPV/DvgaAW/uM+jJ/8qG47DPNYZEaOjKNelOQuSKijXBwQj0LkoLjuqZiy72vTiYCvJ1lj0NaLvsx3sARckDuQNKiEBHbAEGySb8pv51/pwBdiY28O0LQxCfZnKn85N+u1A9DR9QEvFbHGRwomWDXjowTb9VpTwe62WsQy+fyWP0V1HU2y0Jh1RTbIE6TLAI2hbK+oc60FQk4yuiHoTERTy1of70mxTAy/JnSzWV/DgGf0cNE0K6pMi4eRLHAgMBAAECggEAJ+z+asFd4bwkdpeW0dJv6cs274e05bgfSlgg9vzit99LrVGvi+ugSnPz5J7orRU66o2vjscMsi3BHlcoUpDg7ZJXDGNWEEsf5RcYZTxEittebapwHSTt8xA64fHlsq0nKGXqmFJs7ZqgEpV95Np6ZULi1QwtUJZTl/+iR4+7CHor8+7vNF7kaQGX54WlARA1FZ5WK2OavcgQ1TqNnIikLGrM1QCzkU64Y4REq/gCy6oY6ZB55Trsn7oNbkIVm+qi9YojTOugFuv5e0nBCH996pc2iALy/uamrhcDRfyJtezVTF4idYLOuOKueim7C7JVwmcGsRkJY+7qshiOlcTVuQKBgQDs2XbjQkrtPVGHi7ON8ual+OqFuyaalLSWGAxQpW8maweytk9IDQTDctgq+MjVHXQfAiwGLyNVYyJ10EaEArvkolHDYm8sesTMH/MHvRU5z1SnyLX/3DMFUDsQELoUJVuOs7ehDch9jkyykAID6Ekfy6RmhVIZL8qYsbaJaSTDlQKBgQC1fdBTM212ZQAXpjMDEfp3T4heduivxPOaIuo2t71tHBkuza/J7Lz5mnzUWBjM2ozxAG0CJmDgJ1Uq4BL4ASb6B3yj9ARaVxR7ki14VNEaeBOQVnV9sX3d70lco4xwE6llMqahhr4VQhNRgItJoe2bryhwIgM65HU5NMG/qGEl6wKBgQCLg51wSIByNohr23KszsPmSDV9CLy4mZe2QfwfFRPgAXPLZXBV9LU1m9SRm3JRdsQL7++WxPV/OqH12/VtlV6wUpzvBnCONtLtmnRBvrNeFi67lpL+X+r92kCwOeoiK88+w8YK7F+I9eVf2UwWmSFZxU7VxPqTM3+XajcnwfOR8QKBgHSbWE0MnsjRzcRkESRo2a7sl6Zx6cB83zLqOedcGEXRJtDucIJhK22Hd6qKy6xawvFZH7Cg7d83Bj5c53MWcm90ff1tG5rblhpq6fIFNLA39AHpuuU+M6tzQYRWxDWCPHT7nRns05Ye9QCJgXSWqG49lNKu17pv7G0L22wDiB9vAoGAcb5m6PCFzPVGy3R6zzpQNBqNTBOySGP4uY8OaY1vdqOaPaMb20OyzgRFRGY0twHfNGRUSofRBqwY+yXphyCX0Xx5RZhk7PWojx99C1uXZHRTDFw8TpdA4HG0Nx1sQ3UVzwPzM+2uoaqkuTCD2B22z/rekpy3muoLgLNa3XxW95I=";
    
    // 支付宝公钥 (需要从支付宝开发平台获取)
    private String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuV6nhoKjZwYKIJMBeYKxG3cBRTNLvHGpjTTQIMePLOWadgGP8rmuxuDDoU9r+YdiLLdHGsO5voJM1TfeUkiK9+iZ1jD7Ndw2/p+eya2lTX3uQbbiSJTdm1HbNHdiEqNaYq1kYt0fJdPa7XYmHIgu8tpPZN63+4Q63XIr+D9PVT/G+wRyGb5htS4DS47qjDPYoxWQw0BgAAKbPquDLtYIBznk0aolmGI6bYgOMV+zwmuKXpRodJ0vHAUQr+RvQg/8YsDK21QTyBvQKKn1nQOrT0H2ARbVfjdl45sGmSrc5RmR0ztts3gAsR3Xl5ttjsXm9aFqztApKX1nl1HBdER+KwIDAQAB";
    
    // 签名方式
    private String signType = "RSA2";
    
    // 字符编码格式
    private String charset = "UTF-8";
    
    // 返回数据格式
    private String format = "json";
    
    // 沙箱环境标识
    private boolean sandbox = true;
    
    // 异步通知地址 (需要外网可访问)
    private String notifyUrl = "http://your-domain.com/api/payment/alipay/notify";
    
    // 同步返回地址
    private String returnUrl = "http://localhost:3000/payment/success";
    
    // 沙箱买家账号信息 (用于测试)
    private SandboxBuyer sandboxBuyer = new SandboxBuyer();
    
    public static class SandboxBuyer {
        private String loginName = "fntkra7936@sandbox.com";  // 沙箱买家账号
        private String payPassword = "111111";               // 支付密码
        private String userName = "沙箱测试用户";              // 用户姓名
        
        // Getters and Setters
        public String getLoginName() { return loginName; }
        public void setLoginName(String loginName) { this.loginName = loginName; }
        
        public String getPayPassword() { return payPassword; }
        public void setPayPassword(String payPassword) { this.payPassword = payPassword; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
    }
    
    // Getters and Setters
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    
    public String getAppPrivateKey() { return appPrivateKey; }
    public void setAppPrivateKey(String appPrivateKey) { this.appPrivateKey = appPrivateKey; }
    
    public String getAlipayPublicKey() { return alipayPublicKey; }
    public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
    
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
    
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public boolean isSandbox() { return sandbox; }
    public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }
    
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    
    public SandboxBuyer getSandboxBuyer() { return sandboxBuyer; }
    public void setSandboxBuyer(SandboxBuyer sandboxBuyer) { this.sandboxBuyer = sandboxBuyer; }
}