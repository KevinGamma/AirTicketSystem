package com.airticket.service;

import com.airticket.mapper.PaymentMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.model.Payment;
import com.airticket.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationService.class);
    private static final String RESCHEDULE_TRANSFER = "RESCHEDULE_TRANSFER";

    private final PaymentMapper paymentMapper;
    private final TicketMapper ticketMapper;

    public PaymentReconciliationService(PaymentMapper paymentMapper, TicketMapper ticketMapper) {
        this.paymentMapper = paymentMapper;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public boolean ensureRescheduleTransferPayment(Ticket newTicket) {
        if (newTicket == null || newTicket.getId() == null || newTicket.getOriginalTicketId() == null) {
            return false;
        }

        List<Payment> existingPayments = paymentMapper.findByTicketIdOrderByCreatedAtDesc(newTicket.getId());
        if (existingPayments != null && !existingPayments.isEmpty()) {
            return false;
        }

        Ticket originalTicket = ticketMapper.findById(newTicket.getOriginalTicketId());
        if (originalTicket == null) {
            logger.warn("Cannot create transfer payment for ticket {} because original ticket {} was not found",
                newTicket.getId(), newTicket.getOriginalTicketId());
            return false;
        }

        List<Payment> originalPayments = paymentMapper.findByTicketIdOrderByCreatedAtDesc(originalTicket.getId());
        Payment sourcePayment = originalPayments == null || originalPayments.isEmpty() ? null : originalPayments.get(0);

        Payment transferPayment = new Payment();
        transferPayment.setTicketId(newTicket.getId());
        transferPayment.setPaymentNumber(buildTransferPaymentNumber(newTicket.getId()));
        transferPayment.setAlipayTradeNo(sourcePayment != null ? sourcePayment.getAlipayTradeNo() : null);
        transferPayment.setAmount(BigDecimal.ZERO);
        transferPayment.setCurrency(sourcePayment != null && sourcePayment.getCurrency() != null ? sourcePayment.getCurrency() : "CNY");
        transferPayment.setPaymentMethod(RESCHEDULE_TRANSFER);
        transferPayment.setStatus("SUCCESS");
        transferPayment.setSubject("改签支付承接-" + newTicket.getTicketNumber());
        transferPayment.setBody("系统承接原票支付状态，原票ID=" + originalTicket.getId());
        transferPayment.setBuyerLogonId(sourcePayment != null ? sourcePayment.getBuyerLogonId() : null);
        transferPayment.setBuyerUserId(sourcePayment != null ? sourcePayment.getBuyerUserId() : null);
        transferPayment.setPaymentTime(newTicket.getPaymentTime() != null ? newTicket.getPaymentTime() : Instant.now());
        transferPayment.setSandboxMode(sourcePayment != null && sourcePayment.isSandboxMode());
        transferPayment.setSandboxBuyerAccount(sourcePayment != null ? sourcePayment.getSandboxBuyerAccount() : null);

        paymentMapper.insertPayment(transferPayment);
        logger.info("Created reschedule transfer payment for new ticket {} from original ticket {}",
            newTicket.getId(), originalTicket.getId());
        return true;
    }

    @Transactional
    public int repairMissingRescheduleTransferPayments() {
        List<Ticket> paidRescheduledTickets = ticketMapper.findRescheduledTicketsByStatus("PAID");
        int repairedCount = 0;

        for (Ticket ticket : paidRescheduledTickets) {
            try {
                if (ensureRescheduleTransferPayment(ticket)) {
                    repairedCount++;
                }
            } catch (Exception ex) {
                logger.error("Failed to repair transfer payment for ticket {}", ticket.getId(), ex);
            }
        }

        logger.info("Reschedule transfer payment repair completed, repairedCount={}", repairedCount);
        return repairedCount;
    }

    private String buildTransferPaymentNumber(Long ticketId) {
        return "RST" + ticketId + System.currentTimeMillis();
    }
}
