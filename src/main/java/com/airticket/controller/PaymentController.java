package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.dto.PaymentRequest;
import com.airticket.dto.PaymentResponse;
import com.airticket.service.AlipayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private AlipayService alipayService;

    @PostMapping("/alipay/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createAlipayPayment(
            @RequestBody PaymentRequest request) {
        
        try {
            logger.info("创建支付宝支付订单: ticketId={}, amount={}, sandbox={}", 
                       request.getTicketId(), request.getAmount(), request.isUseSandbox());
            
            // 验证请求参数
            if (request.getTicketId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("票据ID不能为空"));
            }
            
            if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("支付金额必须大于0"));
            }
            
            // 默认使用沙箱环境
            if (!request.isUseSandbox()) {
                request.setUseSandbox(true);
                logger.info("自动启用沙箱模式进行测试");
            }
            
            // 创建支付订单
            PaymentResponse paymentResponse = alipayService.createPayment(request);
            
            if (paymentResponse.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(paymentResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(paymentResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("创建支付订单失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("创建支付订单失败: " + e.getMessage()));
        }
    }
    

    @PostMapping("/alipay/notify")
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        try {
            logger.info("收到支付宝异步通知");
            
            // 获取支付宝POST过来的反馈信息
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            
            logger.info("支付宝通知参数: {}", params);
            
            // 处理通知
            String result = alipayService.handleNotify(params);
            
            logger.info("支付宝通知处理结果: {}", result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("处理支付宝通知失败", e);
            return ResponseEntity.ok("fail");
        }
    }

    @GetMapping("/alipay/return")
    public ResponseEntity<ApiResponse<Map<String, String>>> alipayReturn(
            @RequestParam Map<String, String> params) {
        
        try {
            logger.info("支付宝同步返回: {}", params);
            
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");
            
            Map<String, String> result = new HashMap<>();
            result.put("paymentNumber", outTradeNo);
            result.put("alipayTradeNo", tradeNo);
            result.put("amount", totalAmount);
            result.put("status", "支付完成");
            result.put("message", "感谢您的支付，订单处理中...");
            result.put("sandboxMode", "true");
            result.put("tip", "这是沙箱测试支付，未产生真实交易");
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("处理支付宝同步返回失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("处理支付返回失败: " + e.getMessage()));
        }
    }
    

    @GetMapping("/alipay/query/{paymentNumber}")
    public ResponseEntity<ApiResponse<PaymentResponse>> queryPaymentStatus(
            @PathVariable String paymentNumber) {
        
        try {
            logger.info("查询支付状态: {}", paymentNumber);
            
            PaymentResponse response = alipayService.queryPaymentStatus(paymentNumber);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("查询支付状态失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("查询支付状态失败: " + e.getMessage()));
        }
    }

    @GetMapping("/sandbox/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSandboxInfo() {
        try {
            Map<String, Object> sandboxInfo = new HashMap<>();
            sandboxInfo.put("enabled", true);
            sandboxInfo.put("gatewayUrl", "https://openapi.alipaydev.com/gateway.do");
            sandboxInfo.put("testAccount", "fntkra7936@sandbox.com");
            sandboxInfo.put("testPassword", "111111");
            sandboxInfo.put("instructions", new String[]{
                "1. 这是支付宝沙箱测试环境，不会产生真实交易",
                "2. 请使用提供的测试账号进行支付测试", 
                "3. 支付密码为6个1: 111111",
                "4. 测试完成后请验证订单状态是否正确更新"
            });
            sandboxInfo.put("warning", "⚠️ 仅用于开发测试，请勿在生产环境使用");
            
            return ResponseEntity.ok(ApiResponse.success(sandboxInfo));
            
        } catch (Exception e) {
            logger.error("获取沙箱信息失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("获取沙箱信息失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sandbox/simulate-success")
    public ResponseEntity<ApiResponse<String>> simulatePaymentSuccess(
            @RequestParam String paymentNumber) {
        
        try {
            logger.info("模拟支付成功: {}", paymentNumber);
            
            // 模拟支付宝异步通知参数
            Map<String, String> mockNotifyParams = new HashMap<>();
            mockNotifyParams.put("trade_status", "TRADE_SUCCESS");
            mockNotifyParams.put("out_trade_no", paymentNumber);
            mockNotifyParams.put("trade_no", "SANDBOX_TRADE_" + System.currentTimeMillis());
            mockNotifyParams.put("total_amount", "1280.00");
            mockNotifyParams.put("buyer_logon_id", "fntkra7936@sandbox.com");
            
            String result = alipayService.handleNotify(mockNotifyParams);
            
            if ("success".equals(result)) {
                return ResponseEntity.ok(ApiResponse.success("模拟支付成功处理完成"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("模拟支付处理失败"));
            }
            
        } catch (Exception e) {
            logger.error("模拟支付成功失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("模拟支付失败: " + e.getMessage()));
        }
    }

    @PostMapping("/alipay/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @RequestBody Map<String, Object> request) {
        
        try {
            String paymentNumber = (String) request.get("paymentNumber");
            BigDecimal refundAmount = new BigDecimal(request.get("refundAmount").toString());
            String refundReason = (String) request.get("refundReason");
            
            logger.info("处理退款请求: paymentNumber={}, refundAmount={}", paymentNumber, refundAmount);
            
            PaymentResponse response = alipayService.processRefund(paymentNumber, refundAmount, refundReason);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("退款处理成功", response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("退款处理失败: " + response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("退款请求处理失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("退款请求处理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/alipay/refund/ticket/{ticketId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefundByTicketId(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Object> request) {
        
        try {
            BigDecimal refundAmount = new BigDecimal(request.get("refundAmount").toString());
            String refundReason = (String) request.get("refundReason");
            
            logger.info("根据票据ID处理退款: ticketId={}, refundAmount={}", ticketId, refundAmount);
            
            PaymentResponse response = alipayService.processRefundByTicketId(ticketId, refundAmount, refundReason);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("退款处理成功", response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("退款处理失败: " + response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("根据票据ID退款请求处理失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("退款请求处理失败: " + e.getMessage()));
        }
    }
}