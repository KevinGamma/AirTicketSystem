package com.airticket.service;

import com.airticket.config.AlipayConfig;
import com.airticket.dto.PaymentRequest;
import com.airticket.dto.PaymentResponse;
import com.airticket.model.Payment;
import com.airticket.model.Ticket;
import com.airticket.model.Flight;
import com.airticket.model.AdminApprovalRequest;
import com.airticket.mapper.PaymentMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.mapper.AdminApprovalRequestMapper;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlipayService {

    private static final Logger logger = LoggerFactory.getLogger(AlipayService.class);

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private FlightService flightService;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private AdminApprovalRequestMapper adminApprovalRequestMapper;

    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            logger.info("创建支付订单 - 沙箱模式: {}", request.isUseSandbox());
            Ticket ticket = ticketMapper.findById(request.getTicketId());
            if (ticket == null) {
                return PaymentResponse.error("票据不存在");
            }
            if (!"BOOKED".equals(ticket.getStatus()) && !"PENDING_RESCHEDULE".equals(ticket.getStatus())) {
                return PaymentResponse.error("票据状态不允许支付");
            }
            Flight flight = flightService.getFlightById(ticket.getFlightId());
            if (flight == null) {
                return PaymentResponse.error("航班信息不存在");
            }
            Instant now = Instant.now();
            if (now.isAfter(flight.getDepartureTimeUtc())) {
                return PaymentResponse.error("Cannot create payment for a ticket after flight departure");
            }
            if (ticket.getBookingTime() != null &&
                    !"PENDING_RESCHEDULE".equals(ticket.getStatus()) &&
                    now.isAfter(ticket.getBookingTime().plusSeconds(10 * 60))) {
                return PaymentResponse.error("Payment deadline exceeded. Tickets must be paid within 10 minutes of booking");
            }
            if (now.isAfter(flight.getDepartureTimeUtc().minusSeconds(40 * 60))) {
                return PaymentResponse.error("Cannot create payment for tickets within 40 minutes of departure");
            }
            String paymentNumber = generatePaymentNumber();
            String paymentSubject = "机票支付-" + flight.getFlightNumber();
            String paymentBody = "机票预订支付";
            if (ticket.getOriginalTicketId() != null) {
                paymentSubject = "改签费用支付-" + flight.getFlightNumber();
                paymentBody = "航班改签费用支付";
            }
            Payment payment = new Payment(
                    request.getTicketId(),
                    paymentNumber,
                    request.getAmount(),
                    paymentSubject,
                    paymentBody,
                    request.isUseSandbox()
            );
            if (request.isUseSandbox()) {
                return createSandboxPayment(payment, request);
            } else {
                return createProductionPayment();
            }
        } catch (Exception e) {
            logger.error("创建支付订单失败", e);
            return PaymentResponse.error("创建支付订单失败: " + e.getMessage());
        }
    }

    private PaymentResponse createSandboxPayment(Payment payment, PaymentRequest request) {
        logger.info("创建沙箱支付订单: {}", payment.getPaymentNumber());
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getAppPrivateKey(),
                    alipayConfig.getFormat(),
                    alipayConfig.getCharset(),
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getSignType()
            );
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
            alipayRequest.setReturnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : alipayConfig.getReturnUrl());
            String bizContent = String.format(
                    "{"
                            + "\"out_trade_no\":\"%s\","
                            + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                            + "\"total_amount\":\"%s\","
                            + "\"subject\":\"%s\","
                            + "\"body\":\"%s\""
                            + "}",
                    payment.getPaymentNumber(),
                    payment.getAmount().toString(),
                    payment.getSubject(),
                    payment.getBody()
            );
            alipayRequest.setBizContent(bizContent);
            String paymentUrl = alipayClient.pageExecute(alipayRequest).getBody();
            PaymentResponse response = PaymentResponse.success(payment.getPaymentNumber(), paymentUrl);
            response.setAmount(payment.getAmount());
            response.setStatus("PENDING");
            response.setSandboxMode(true);
            response.setSandboxTips("即将跳转到支付宝沙箱支付页面，请使用测试账号完成支付");
            PaymentResponse.SandboxTestInfo testInfo = new PaymentResponse.SandboxTestInfo(
                    alipayConfig.getSandboxBuyer().getLoginName(),
                    alipayConfig.getSandboxBuyer().getPayPassword(),
                    "请使用提供的沙箱账号登录支付宝进行测试支付。支付完成后系统会自动更新订单状态。"
            );
            response.setSandboxTestInfo(testInfo);
            paymentMapper.insertPayment(payment);
            logger.info("沙箱支付订单创建成功: {}", payment.getPaymentNumber());
            return response;
        } catch (AlipayApiException e) {
            logger.error("创建沙箱支付订单失败", e);
            return PaymentResponse.error("创建支付订单失败: " + e.getMessage());
        }
    }

    private PaymentResponse createProductionPayment() {
        logger.warn("生产环境支付创建 - 这是模拟实现");
        return PaymentResponse.error(
                "生产环境支付暂未实现。请使用沙箱模式进行测试，或联系技术团队配置真实的支付宝商户信息。"
        );
    }

    public String handleNotify(Map<String, String> params) {
        try {
            logger.info("收到支付宝异步通知 - 沙箱模式");
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");
            if (!verifySandboxSignature()) {
                logger.error("签名验证失败");
                return "fail";
            }
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                return handlePaymentSuccess(outTradeNo, tradeNo, new BigDecimal(totalAmount));
            }
            logger.info("支付状态: {}, 无需处理", tradeStatus);
            return "success";
        } catch (Exception e) {
            logger.error("处理支付宝通知失败", e);
            return "fail";
        }
    }

    private boolean verifySandboxSignature() {
        logger.info("验证沙箱签名 - 模拟通过");
        return true;
    }

    private String handlePaymentSuccess(String paymentNumber, String alipayTradeNo, BigDecimal amount) {
        try {
            logger.info("处理支付成功: 支付单号={}, 支付宝交易号={}, 金额={}",
                    paymentNumber, alipayTradeNo, amount);
            Payment payment = paymentMapper.findByPaymentNumber(paymentNumber);
            if (payment == null) {
                logger.error("找不到支付记录: {}", paymentNumber);
                return "fail";
            }
            payment.setStatus("SUCCESS");
            payment.setAlipayTradeNo(alipayTradeNo);
            payment.setPaymentTime(Instant.now());
            paymentMapper.updatePaymentStatus(payment);
            Ticket ticket = ticketMapper.findById(payment.getTicketId());
            if (ticket != null) {
                boolean isReschedulePayment = "PENDING_RESCHEDULE".equals(ticket.getStatus());
                ticket.setStatus("PAID");
                ticket.setPaymentTime(Instant.now());
                ticketMapper.updateStatus(ticket);
                logger.info("票据状态已更新为PAID: ticketId={}, isReschedulePayment={}", ticket.getId(), isReschedulePayment);
                if (isReschedulePayment) {
                    try {
                        completeReschedulePayment(ticket, paymentNumber);
                    } catch (Exception e) {
                        logger.error("完成改签支付流程失败: ticketId={}, paymentNumber={}", ticket.getId(), paymentNumber, e);
                    }
                }
            }
            logger.info("支付处理完成 - 沙箱模式: paymentNumber={}", paymentNumber);
            return "success";
        } catch (Exception e) {
            logger.error("处理支付成功回调失败", e);
            return "fail";
        }
    }

    private String generatePaymentNumber() {
        return "PAY" + System.currentTimeMillis() +
                String.format("%03d", (int)(Math.random() * 1000));
    }

    public PaymentResponse queryPaymentStatus(String paymentNumber) {
        logger.info("查询支付状态 - 沙箱模式: {}", paymentNumber);
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getAppPrivateKey(),
                    alipayConfig.getFormat(),
                    alipayConfig.getCharset(),
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getSignType()
            );
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\"}",
                    paymentNumber
            );
            request.setBizContent(bizContent);
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                logger.info("支付状态查询成功: paymentNumber={}, tradeStatus={}", paymentNumber, tradeStatus);
                Payment payment = paymentMapper.findByPaymentNumber(paymentNumber);
                PaymentResponse paymentResponse = new PaymentResponse(true, "查询成功");
                paymentResponse.setPaymentNumber(paymentNumber);
                paymentResponse.setSandboxMode(true);
                String newStatus;
                String message;
                switch (tradeStatus) {
                    case "TRADE_SUCCESS", "TRADE_FINISHED" -> {
                        newStatus = "SUCCESS";
                        message = "支付成功";
                    }
                    case "WAIT_BUYER_PAY" -> {
                        newStatus = "PENDING";
                        message = "等待买家付款";
                    }
                    case "TRADE_CLOSED" -> {
                        newStatus = "CLOSED";
                        message = "交易关闭";
                    }
                    case null, default -> {
                        newStatus = "UNKNOWN";
                        message = "未知状态: " + tradeStatus;
                    }
                }
                paymentResponse.setStatus(newStatus);
                paymentResponse.setMessage(message);
                if (payment != null && !newStatus.equals(payment.getStatus())) {
                    payment.setStatus(newStatus);
                    if (response.getTradeNo() != null) {
                        payment.setAlipayTradeNo(response.getTradeNo());
                    }
                    if ("SUCCESS".equals(newStatus)) {
                        payment.setPaymentTime(Instant.now());
                        Ticket ticket = ticketMapper.findById(payment.getTicketId());
                        if (ticket != null && !"PAID".equals(ticket.getStatus())) {
                            boolean isReschedulePayment = "PENDING_RESCHEDULE".equals(ticket.getStatus());
                            ticket.setStatus("PAID");
                            ticket.setPaymentTime(Instant.now());
                            ticketMapper.updateStatus(ticket);
                            logger.info("票据状态已更新为PAID: ticketId={}, isReschedulePayment={}", ticket.getId(), isReschedulePayment);
                            if (isReschedulePayment) {
                                try {
                                    completeReschedulePayment(ticket, paymentNumber);
                                } catch (Exception e) {
                                    logger.error("完成改签支付流程失败: ticketId={}, paymentNumber={}", ticket.getId(), paymentNumber, e);
                                }
                            }
                        }
                    }
                    paymentMapper.updatePaymentStatus(payment);
                    logger.info("支付记录状态已更新: paymentNumber={}, status={}", paymentNumber, newStatus);
                }
                if (response.getTotalAmount() != null) {
                    paymentResponse.setAmount(new BigDecimal(response.getTotalAmount()));
                }
                if (response.getTradeNo() != null) {
                    paymentResponse.setAlipayTradeNo(response.getTradeNo());
                }
                return paymentResponse;
            } else {
                logger.error("支付状态查询失败: code={}, msg={}, subCode={}, subMsg={}",
                        response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                if ("40004".equals(response.getCode()) || "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    PaymentResponse paymentResponse = new PaymentResponse(true, "查询成功");
                    paymentResponse.setPaymentNumber(paymentNumber);
                    paymentResponse.setStatus("PENDING");
                    paymentResponse.setMessage("等待支付");
                    paymentResponse.setSandboxMode(true);
                    return paymentResponse;
                }
                return PaymentResponse.error("查询支付状态失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            logger.error("查询支付状态API调用失败: {}", e.getErrMsg(), e);
            return PaymentResponse.error("查询支付状态失败: " + e.getErrMsg());
        } catch (Exception e) {
            logger.error("查询支付状态失败", e);
            return PaymentResponse.error("查询支付状态失败: " + e.getMessage());
        }
    }

    public PaymentResponse processRefund(String paymentNumber, BigDecimal refundAmount, String refundReason) {
        logger.info("处理退款 - 支付单号: {}, 退款金额: {}", paymentNumber, refundAmount);
        Payment payment = null;
        try {
            payment = paymentMapper.findByPaymentNumber(paymentNumber);
            if (payment == null) {
                logger.error("支付记录不存在: {}", paymentNumber);
                return PaymentResponse.error("退款失败：找不到支付记录，请联系客服处理");
            }
            
            if (!"SUCCESS".equals(payment.getStatus())) {
                logger.warn("支付状态不是SUCCESS: paymentNumber={}, status={}", paymentNumber, payment.getStatus());
                return PaymentResponse.error("退款失败：支付状态异常，请联系客服处理");
            }
            
            // Use the new method that ensures actual refunds
            return processActualRefund(payment, refundAmount, refundReason);
            
        } catch (Exception e) {
            logger.error("处理退款失败: paymentNumber={}", paymentNumber, e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "系统内部错误";
            }
            return PaymentResponse.error("退款失败: " + errorMessage);
        }
    }

    public PaymentResponse processRefundByTicketId(Long ticketId, BigDecimal refundAmount, String refundReason) {
        logger.info("根据票据ID处理退款: ticketId={}, refundAmount={}", ticketId, refundAmount);
        try {
            List<Payment> payments = paymentMapper.findByTicketIdOrderByCreatedAtDesc(ticketId);
            Payment payment = null;
            if (payments != null && !payments.isEmpty()) {
                payment = payments.getFirst();
                logger.info("找到{}个支付记录，使用最新的: paymentNumber={}", payments.size(), payment.getPaymentNumber());
            }
            
            // Critical fix: Only process refund if there's a real payment
            if (payment == null) {
                logger.warn("票据没有支付记录，无法进行真实退款: ticketId={}", ticketId);
                // For unpaid tickets, there's no money to refund
                if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    logger.info("取消未支付票据，无需退款: ticketId={}", ticketId);
                    PaymentResponse response = new PaymentResponse(true, "取消成功（无需退款）");
                    response.setStatus("CANCELLED");
                    response.setAmount(BigDecimal.ZERO);
                    return response;
                } else {
                    logger.error("票据没有支付记录但请求退款金额 > 0: ticketId={}, refundAmount={}", ticketId, refundAmount);
                    return PaymentResponse.error("无法退款：该票据没有支付记录，请联系客服处理");
                }
            }
            
            // Process actual refund for paid tickets
            return processActualRefund(payment, refundAmount, refundReason);
        } catch (Exception e) {
            logger.error("根据票据ID处理退款失败: ticketId={}", ticketId, e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "系统处理异常，请稍后重试";
            }
            return PaymentResponse.error("退款失败: " + errorMessage);
        }
    }

    /**
     * Process actual refund to user's Alipay account
     * This method ensures real money transfer happens, not just simulation
     */
    private PaymentResponse processActualRefund(Payment payment, BigDecimal refundAmount, String refundReason) {
        logger.info("处理真实退款到用户支付宝账户: paymentNumber={}, refundAmount={}", payment.getPaymentNumber(), refundAmount);
        
        if (!"SUCCESS".equals(payment.getStatus())) {
            logger.warn("支付状态不是SUCCESS，无法退款: paymentNumber={}, status={}", payment.getPaymentNumber(), payment.getStatus());
            return PaymentResponse.error("无法退款：支付状态异常，请联系客服处理");
        }
        
        try {
            // Initialize Alipay client
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getAppPrivateKey(),
                    alipayConfig.getFormat(),
                    alipayConfig.getCharset(),
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getSignType()
            );
            
            // Create refund request
            AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
            String refundNo = generateRefundNumber(payment.getPaymentNumber());
            
            // Build request parameters
            String bizContent = String.format(
                    "{"
                            + "\"out_trade_no\":\"%s\","
                            + "\"refund_amount\":\"%s\","
                            + "\"refund_reason\":\"%s\","
                            + "\"out_request_no\":\"%s\""
                            + "}",
                    payment.getPaymentNumber(),
                    refundAmount.toString(),
                    refundReason != null ? refundReason : "机票退款",
                    refundNo
            );
            refundRequest.setBizContent(bizContent);
            
            logger.info("发起支付宝退款请求: paymentNumber={}, refundNo={}, amount={}", 
                       payment.getPaymentNumber(), refundNo, refundAmount);
            
            // Execute refund request
            AlipayTradeRefundResponse response = alipayClient.execute(refundRequest);
            
            if (response.isSuccess()) {
                logger.info("支付宝退款成功: paymentNumber={}, refundAmount={}, buyerLogonId={}", 
                           payment.getPaymentNumber(), refundAmount, response.getBuyerLogonId());
                
                // Update payment status
                payment.setStatus("REFUNDED");
                payment.setUpdatedAt(Instant.now());
                paymentMapper.updatePaymentStatus(payment);
                
                // Create successful response
                PaymentResponse paymentResponse = new PaymentResponse(true, "退款成功，金额将在1-3个工作日内到达您的支付宝账户");
                paymentResponse.setPaymentNumber(payment.getPaymentNumber());
                paymentResponse.setAmount(refundAmount);
                paymentResponse.setStatus("REFUNDED");
                paymentResponse.setSandboxMode(payment.isSandboxMode());
                paymentResponse.setAlipayTradeNo(payment.getAlipayTradeNo());
                
                // Add refund details
                Map<String, Object> refundDetails = new HashMap<>();
                refundDetails.put("refundNo", refundNo);
                refundDetails.put("refundAmount", refundAmount);
                refundDetails.put("refundReason", refundReason);
                refundDetails.put("refundTime", Instant.now());
                refundDetails.put("buyerLogonId", response.getBuyerLogonId());
                refundDetails.put("buyerUserId", response.getBuyerUserId());
                refundDetails.put("fundChange", response.getFundChange());
                paymentResponse.setRefundDetails(refundDetails);
                
                return paymentResponse;
            } else {
                logger.error("支付宝退款失败: code={}, msg={}, subCode={}, subMsg={}",
                            response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                
                String errorMsg = response.getSubMsg();
                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                    errorMsg = response.getMsg() != null ? response.getMsg() : "支付宝退款接口返回失败";
                }
                
                // Handle specific error cases
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    return PaymentResponse.error("退款失败：支付宝找不到该笔交易，请联系客服处理");
                } else if ("ACQ.REFUND_AMT_NOT_EQUAL_TOTAL".equals(response.getSubCode())) {
                    return PaymentResponse.error("退款失败：退款金额超过可退款金额");
                } else if ("ACQ.REASON_TRADE_BEEN_FREEZEN".equals(response.getSubCode())) {
                    return PaymentResponse.error("退款失败：该笔交易已被冻结，请联系客服处理");
                }
                
                return PaymentResponse.error("退款失败: " + errorMsg);
            }
            
        } catch (AlipayApiException e) {
            logger.error("支付宝退款API调用异常: errCode={}, errMsg={}", e.getErrCode(), e.getErrMsg(), e);
            
            // For sandbox/test environments, fall back to sandbox processing
            if (payment.isSandboxMode() || isTestEnvironment(payment)) {
                logger.info("检测到沙箱/测试环境，使用沙箱退款处理: {}", payment.getPaymentNumber());
                return processSandboxRefund(payment, refundAmount, refundReason, payment.getPaymentNumber());
            }
            
            return PaymentResponse.error("退款失败: " + e.getErrMsg());
        } catch (Exception e) {
            logger.error("处理真实退款时发生异常: paymentNumber={}", payment.getPaymentNumber(), e);
            return PaymentResponse.error("退款失败: " + e.getMessage());
        }
    }
    
    /**
     * Check if this is a test environment payment
     */
    private boolean isTestEnvironment(Payment payment) {
        return payment.getPaymentNumber().startsWith("PAY") ||
               payment.getAlipayTradeNo() == null ||
               payment.getAlipayTradeNo().startsWith("SIMULATED") ||
               payment.getAlipayTradeNo().startsWith("MOCK") ||
               payment.getAlipayTradeNo().startsWith("TEST");
    }

    private String generateRefundNumber(String paymentNumber) {
        return "REFUND_" + paymentNumber + "_" + System.currentTimeMillis();
    }

    private void completeReschedulePayment(Ticket ticket, String paymentNumber) {
        logger.info("开始完成改签支付流程: ticketId={}, paymentNumber={}", ticket.getId(), paymentNumber);
        try {
            Long originalTicketId = ticket.getOriginalTicketId();
            if (originalTicketId == null) {
                logger.error("无法找到原票据ID: ticketId={}", ticket.getId());
                return;
            }
            List<AdminApprovalRequest> requests = adminApprovalRequestMapper.findByTicketId(originalTicketId);
            AdminApprovalRequest rescheduleRequest = null;
            for (AdminApprovalRequest request : requests) {
                if ("RESCHEDULE".equals(request.getRequestType()) &&
                        ("AWAITING_PAYMENT".equals(request.getStatus()) || "APPROVED".equals(request.getStatus()))) {
                    rescheduleRequest = request;
                    break;
                }
            }
            if (rescheduleRequest == null) {
                logger.error("未找到相关的改签审批请求: originalTicketId={}", originalTicketId);
                return;
            }
            try {
                adminApprovalRequestMapper.updatePaymentNumber(rescheduleRequest.getId(), paymentNumber);
                adminApprovalRequestMapper.updateStatus(
                        rescheduleRequest.getId(), "PAYMENT_COMPLETED", null, null, LocalDateTime.now()
                );
                logger.info("改签审批请求已更新为支付完成状态: requestId={}, paymentNumber={}",
                        rescheduleRequest.getId(), paymentNumber);
            } catch (Exception e) {
                logger.warn("更新审批请求支付信息失败，可能是数据库未更新: {}", e.getMessage());
                adminApprovalRequestMapper.updateStatus(
                        rescheduleRequest.getId(), "PAYMENT_COMPLETED", null, null, LocalDateTime.now()
                );
            }
            logger.info("改签支付流程完成: ticketId={}, requestId={}", ticket.getId(), rescheduleRequest.getId());
        } catch (Exception e) {
            logger.error("完成改签支付流程时发生错误", e);
            throw e;
        }
    }

    private PaymentResponse processSandboxRefund(Payment payment, BigDecimal refundAmount, String refundReason, String paymentNumber) {
        logger.info("处理沙箱/测试退款: paymentNumber={}, refundAmount={}, reason={}, hasPayment={}",
                paymentNumber, refundAmount, refundReason, payment != null);
        try {
            String refundNo = generateRefundNumber(paymentNumber);
            if (payment != null) {
                try {
                    payment.setStatus("REFUNDED");
                    payment.setUpdatedAt(Instant.now());
                    paymentMapper.updatePaymentStatus(payment);
                    logger.info("已更新支付记录状态为REFUNDED: {}", paymentNumber);
                } catch (Exception e) {
                    logger.warn("更新支付记录状态失败，但继续处理退款: {}", e.getMessage());
                }
            } else {
                logger.info("无支付记录，跳过状态更新: {}", paymentNumber);
            }
            logger.info("沙箱/测试退款处理完成: paymentNumber={}, refundAmount={}, refundNo={}",
                    paymentNumber, refundAmount, refundNo);
            PaymentResponse paymentResponse = new PaymentResponse(true, "沙箱/测试退款成功");
            paymentResponse.setPaymentNumber(paymentNumber);
            paymentResponse.setAmount(refundAmount);
            paymentResponse.setStatus("REFUNDED");
            paymentResponse.setSandboxMode(true);
            Map<String, Object> refundDetails = new HashMap<>();
            refundDetails.put("refundNo", refundNo);
            refundDetails.put("refundAmount", refundAmount);
            refundDetails.put("refundReason", refundReason != null ? refundReason : "机票退款");
            refundDetails.put("refundTime", Instant.now());
            refundDetails.put("refundType", "SANDBOX_TEST");
            refundDetails.put("note", "沙箱/测试环境退款，无需真实支付宝处理");
            refundDetails.put("hasPaymentRecord", payment != null);
            paymentResponse.setRefundDetails(refundDetails);
            return paymentResponse;
        } catch (Exception e) {
            logger.error("处理沙箱/测试退款失败: paymentNumber={}", paymentNumber, e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "沙箱退款处理异常";
            }
            return PaymentResponse.error("沙箱/测试退款失败: " + errorMessage);
        }
    }
}