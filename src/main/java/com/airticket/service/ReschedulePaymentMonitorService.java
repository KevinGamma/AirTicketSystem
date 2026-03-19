package com.airticket.service;

import com.airticket.mapper.PaymentMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.mapper.AdminApprovalRequestMapper;
import com.airticket.model.Payment;
import com.airticket.model.Ticket;
import com.airticket.model.AdminApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
public class ReschedulePaymentMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReschedulePaymentMonitorService.class);
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private PaymentMapper paymentMapper;
    
    @Autowired
    private AdminApprovalRequestMapper adminApprovalRequestMapper;
    
    @Autowired
    private AlipayService alipayService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedDelay = 10000) 
    public void monitorPendingReschedulePayments() {
        runPaymentMonitoring();
    }

    public void runPaymentMonitoring() {
        try {
            List<Ticket> pendingTickets = ticketMapper.findByStatus("PENDING_RESCHEDULE");
            
            if (pendingTickets.isEmpty()) {
                logger.info("No tickets found in PENDING_RESCHEDULE status");
                return;
            }
            
            logger.info("Found {} tickets in PENDING_RESCHEDULE status, checking payment status...", pendingTickets.size());
            
            for (Ticket ticket : pendingTickets) {
                try {
                    checkAndCompleteReschedulePayment(ticket);
                } catch (Exception e) {
                    logger.error("Error checking reschedule payment for ticket {}: {}", ticket.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in reschedule payment monitoring job", e);
        }
    }

    @Transactional
    protected void checkAndCompleteReschedulePayment(Ticket ticket) {
        List<Payment> payments = paymentMapper.findByTicketIdOrderByCreatedAtDesc(ticket.getId());
        
        if (payments.isEmpty()) {
            logger.debug("No payment records found for ticket {}", ticket.getId());
            return;
        }

        Payment latestPayment = payments.get(0);
        
        logger.debug("Checking ticket {} with payment status: {}", ticket.getId(), latestPayment.getStatus());

        if ("SUCCESS".equals(latestPayment.getStatus()) && "PENDING_RESCHEDULE".equals(ticket.getStatus())) {
            logger.info("Found completed payment for PENDING_RESCHEDULE ticket {}, triggering completion logic", ticket.getId());
            
            try {
                handleReschedulePaymentCompletion(ticket, latestPayment.getPaymentNumber());
            } catch (Exception e) {
                logger.error("Failed to complete reschedule payment for ticket {}: {}", ticket.getId(), e.getMessage());
            }
        } else {
            try {
                String paymentNumber = latestPayment.getPaymentNumber();
                logger.info("Actively querying Alipay for payment number: {} (current local status: {})", 
                           paymentNumber, latestPayment.getStatus());

                alipayService.queryPaymentStatus(paymentNumber);

                Payment updatedPayment = paymentMapper.findByPaymentNumber(paymentNumber);
                if (updatedPayment != null && "SUCCESS".equals(updatedPayment.getStatus()) && 
                    "PENDING_RESCHEDULE".equals(ticket.getStatus())) {
                    logger.info("Payment status updated to SUCCESS, completing reschedule for ticket {}", ticket.getId());
                    handleReschedulePaymentCompletion(ticket, paymentNumber);
                } else {
                    Instant paymentCreatedAt = updatedPayment != null ? updatedPayment.getCreatedAt() : latestPayment.getCreatedAt();
                    if (paymentCreatedAt != null) {
                        Duration timeSincePayment = Duration.between(paymentCreatedAt, Instant.now());
                        if (timeSincePayment.toMinutes() >= 2) {
                            logger.warn("Payment {} is older than 2 minutes and still not completed, forcing status check", paymentNumber);
                            if (Boolean.TRUE.equals(latestPayment.isSandboxMode())) {
                                logger.info("Sandbox payment detected, auto-completing stuck payment after 2 minutes");
                                try {
                                    forceSandboxPaymentCompletion(ticket, latestPayment);
                                } catch (Exception e) {
                                    logger.error("Failed to force sandbox payment completion: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                logger.debug("Failed to query payment status for ticket {}: {}", ticket.getId(), e.getMessage());
            }
        }
    }

    private void handleReschedulePaymentCompletion(Ticket ticket, String paymentNumber) {
        logger.info("Processing reschedule payment completion: ticketId={}, paymentNumber={}", ticket.getId(), paymentNumber);
        
        try {
            ticket.setStatus("PAID");
            ticket.setPaymentTime(Instant.now());
            ticketMapper.updateStatus(ticket);

            Long originalTicketId = ticket.getOriginalTicketId();
            if (originalTicketId != null) {
                List<AdminApprovalRequest> requests = adminApprovalRequestMapper.findByTicketId(originalTicketId);
                
                AdminApprovalRequest rescheduleRequest = requests.stream()
                    .filter(r -> "RESCHEDULE".equals(r.getRequestType()) 
                              && ("AWAITING_PAYMENT".equals(r.getStatus()) || "APPROVED".equals(r.getStatus())))
                    .findFirst()
                    .orElse(null);
                    
                if (rescheduleRequest != null) {
                    try {
                        adminApprovalRequestMapper.updatePaymentNumber(rescheduleRequest.getId(), paymentNumber);
                    } catch (Exception e) {
                        logger.warn("Failed to update payment number, database field may not exist: {}", e.getMessage());
                    }
                    
                    adminApprovalRequestMapper.updateStatus(
                        rescheduleRequest.getId(), "PAYMENT_COMPLETED", null, null, LocalDateTime.now()
                    );

                    Ticket originalTicket = ticketMapper.findById(originalTicketId);
                    notificationService.createReschedulePaymentCompletedNotification(
                        rescheduleRequest,
                        originalTicket,
                        ticket
                    );
                    
                    logger.info("Reschedule payment completion processed successfully: ticketId={}, requestId={}", 
                               ticket.getId(), rescheduleRequest.getId());
                } else {
                    logger.warn("No matching reschedule request found for originalTicketId: {}", originalTicketId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing reschedule payment completion", e);
            throw e;
        }
    }

    @Transactional
    protected void forceSandboxPaymentCompletion(Ticket ticket, Payment payment) {
        logger.info("Forcing sandbox payment completion: ticketId={}, paymentNumber={}", ticket.getId(), payment.getPaymentNumber());
        
        try {
            payment.setStatus("SUCCESS");
            payment.setPaymentTime(Instant.now());
            payment.setAlipayTradeNo("SANDBOX_TRADE_" + System.currentTimeMillis());
            paymentMapper.updatePaymentStatus(payment);
            logger.info("Payment status forcibly updated to SUCCESS");

            ticket.setStatus("PAID");
            ticket.setPaymentTime(Instant.now());
            ticketMapper.updateStatus(ticket);
            logger.info("Ticket status updated to PAID");

            handleReschedulePaymentCompletion(ticket, payment.getPaymentNumber());
            logger.info("Reschedule payment completion handled successfully");
            
        } catch (Exception e) {
            logger.error("Failed to force sandbox payment completion", e);
            throw e;
        }
    }
}
