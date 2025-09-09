package com.airticket.service;

import com.airticket.mapper.AdminApprovalRequestMapper;
import com.airticket.mapper.PaymentMapper;
import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.UserMapper;
import com.airticket.model.AdminApprovalRequest;
import com.airticket.model.Ticket;
import com.airticket.model.Payment;
import com.airticket.model.Flight;
import com.airticket.dto.RescheduleFeeInfo;
import com.airticket.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminApprovalService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminApprovalService.class);
    
    @Autowired
    private AdminApprovalRequestMapper adminApprovalRequestMapper;
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private FlightService flightService;
    
    @Autowired
    private PaymentMapper paymentMapper;
    
    @Autowired
    private FlightMapper flightMapper;
    
    @Autowired
    private AlipayService alipayService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserMapper userMapper;
    
    public List<AdminApprovalRequest> getAllRequests() {
        return adminApprovalRequestMapper.findAll();
    }
    
    public List<AdminApprovalRequest> getPendingRequests() {
        return adminApprovalRequestMapper.findByStatus("PENDING");
    }
    
    public List<AdminApprovalRequest> getRequestsByUserId(Long userId) {
        return adminApprovalRequestMapper.findByUserId(userId);
    }
    
    public AdminApprovalRequest getRequestById(Long id) {
        return adminApprovalRequestMapper.findById(id);
    }
    
    public List<AdminApprovalRequest> getPendingRequestsByTicketId(Long ticketId) {
        return adminApprovalRequestMapper.findPendingRequestsByTicketId(ticketId);
    }
    
    @Transactional
    public AdminApprovalRequest createRefundRequest(Long ticketId, Long userId, String reason) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        // Allow refund requests for both PAID and BOOKED tickets
        if (!"PAID".equals(ticket.getStatus()) && !"BOOKED".equals(ticket.getStatus())) {
            throw new RuntimeException("Only paid or booked tickets can be refunded/cancelled");
        }
        
        // Check for existing pending requests
        List<AdminApprovalRequest> pendingRequests = adminApprovalRequestMapper.findPendingRequestsByTicketId(ticketId);
        if (!pendingRequests.isEmpty()) {
            AdminApprovalRequest existingRequest = pendingRequests.get(0);
            String requestTypeName = getRequestTypeName(existingRequest.getRequestType());
            throw new RuntimeException("A " + requestTypeName + " request is already pending for this ticket. " +
                                     "Please wait for the current request to be processed or cancel it before submitting a new request.");
        }
        
        BigDecimal serviceFee = ticketService.calculateRefundServiceFee(ticket);
        
        // Determine request type based on ticket status
        String requestType = "PAID".equals(ticket.getStatus()) ? "REFUND" : "CANCEL_AND_REFUND";
        
        AdminApprovalRequest request = new AdminApprovalRequest(
            ticketId, userId, requestType, reason, serviceFee, null
        );
        
        adminApprovalRequestMapper.insert(request);
        return request;
    }
    
    @Transactional
    public AdminApprovalRequest createRescheduleRequest(Long ticketId, Long userId, Long newFlightId, String reason) {
        return createRescheduleRequest(ticketId, userId, newFlightId, reason, null);
    }
    
    @Transactional
    public AdminApprovalRequest createRescheduleRequest(Long ticketId, Long userId, Long newFlightId, String reason, List<Long> connectingFlightIds) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        if (!"PAID".equals(ticket.getStatus())) {
            throw new RuntimeException("Only paid tickets can be rescheduled");
        }
        
        // Check for existing pending requests
        List<AdminApprovalRequest> pendingRequests = adminApprovalRequestMapper.findPendingRequestsByTicketId(ticketId);
        if (!pendingRequests.isEmpty()) {
            AdminApprovalRequest existingRequest = pendingRequests.get(0);
            String requestTypeName = getRequestTypeName(existingRequest.getRequestType());
            throw new RuntimeException("A " + requestTypeName + " request is already pending for this ticket. " +
                                     "Please wait for the current request to be processed or cancel it before submitting a new request.");
        }
        
        // Validate the new flight - use enhanced validation that supports connecting flights
        Ticket currentTicket = ticketService.getTicketById(ticketId);
        if (!isValidRescheduleDestination(currentTicket, newFlightId)) {
            throw new RuntimeException("Invalid flight selected for rescheduling");
        }
        
        BigDecimal serviceFee = ticketService.calculateRescheduleServiceFee(ticket);
        
        AdminApprovalRequest request = new AdminApprovalRequest(
            ticketId, userId, "RESCHEDULE", reason, serviceFee, newFlightId
        );
        
        adminApprovalRequestMapper.insert(request);
        return request;
    }
    
    /**
     * 获取审批请求的详细费用信息 - 用于管理员审批时查看
     */
    public RescheduleFeeInfo getApprovalRequestFeeInfo(Long requestId) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Approval request not found");
        }
        
        if (!"RESCHEDULE".equals(request.getRequestType())) {
            throw new RuntimeException("This request is not a reschedule request");
        }
        
        if (request.getNewFlightId() == null) {
            throw new RuntimeException("No new flight specified in the request");
        }
        
        return ticketService.calculateRescheduleFeeInfo(request.getTicketId(), request.getNewFlightId());
    }
    
    @Transactional
    public AdminApprovalRequest approveRequest(Long requestId, Long adminId) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is not pending");
        }
        
        if ("REFUND".equals(request.getRequestType()) || "CANCEL_AND_REFUND".equals(request.getRequestType())) {
            // Process refund or cancellation immediately
            adminApprovalRequestMapper.updateStatus(
                requestId, "APPROVED", adminId, null, LocalDateTime.now()
            );
            ticketService.refundTicket(request.getTicketId(), request.getReason());
            
            // Send refund notification
            try {
                Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                if (originalTicket != null && originalTicket.getPrice() != null) {
                    notificationService.createRefundProcessedNotification(
                        request.getUserId(),
                        request.getTicketId(),
                        originalTicket.getPrice(),
                        originalTicket.getTicketNumber()
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to send refund notification for request " + requestId, e);
            }
        } else if ("RESCHEDULE".equals(request.getRequestType())) {
            // Calculate payment/refund amounts for reschedule
            RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(request.getTicketId(), request.getNewFlightId());
            
            try {
                // Always create new ticket with PENDING_RESCHEDULE status first
                // Then handle payment or refund logic
                if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
                    // Payment required - create new ticket with PENDING_RESCHEDULE status
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                        requestId, "AWAITING_PAYMENT", adminId, null, LocalDateTime.now(),
                        feeInfo.getTotalAdditionalCost(), null, "PAYMENT_REQUIRED"
                    );
                    // Create new ticket with PENDING_RESCHEDULE status (payment required)
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), feeInfo.getTotalAdditionalCost());
                    
                    // Send reschedule approval notification (payment required)
                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        request.setAdditionalFee(feeInfo.getTotalAdditionalCost()); // Set fee for notification
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                } else if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                    // Refund required - but still create with PENDING_RESCHEDULE first, then immediately set to PAID
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                        requestId, "APPROVED", adminId, null, LocalDateTime.now(),
                        null, feeInfo.getTotalRefund(), "REFUND_REQUIRED"
                    );
                    
                    // Create new ticket with PENDING_RESCHEDULE status, then immediately set to PAID (refund case)
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    // Immediately update status to PAID since customer gets refunded
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                    processRescheduleRefund(request, feeInfo.getTotalRefund());
                    
                    // Send reschedule approval notification (refund difference)
                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        request.setRefundAmount(feeInfo.getTotalRefund()); // Set refund for notification
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);
                        
                        // Schedule flight reminder for the new ticket
                        notificationService.scheduleFlightReminder(newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                } else {
                    // No additional payment needed - create with PENDING_RESCHEDULE then immediately set to PAID
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                        requestId, "APPROVED", adminId, null, LocalDateTime.now(),
                        null, null, "NOT_REQUIRED"
                    );
                    // Create new ticket with PENDING_RESCHEDULE status, then immediately set to PAID
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    // Immediately update status to PAID since no additional payment needed
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                    
                    // Send reschedule approval notification (no additional fee)
                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);
                        
                        // Schedule flight reminder for the new ticket
                        notificationService.scheduleFlightReminder(newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                }
            } catch (Exception e) {
                logger.error("Database not updated with payment fields, falling back to pending status approach", e);
                // Fallback: Use the old simple approval method if database hasn't been updated
                adminApprovalRequestMapper.updateStatus(
                    requestId, "APPROVED", adminId, null, LocalDateTime.now()
                );
                
                // Fallback: Always use pending status approach even without payment fields
                if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                    // Refund required - create with PENDING_RESCHEDULE then immediately set to PAID
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                    processRescheduleRefund(request, feeInfo.getTotalRefund());
                } else if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
                    // Payment required - create with PENDING_RESCHEDULE status
                    ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), feeInfo.getTotalAdditionalCost());
                } else {
                    // No payment required and no refund - create with PENDING_RESCHEDULE then immediately set to PAID
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                }
                
                // Log the fee information for manual processing if needed
                logger.info("Reschedule approved with fee info - AdditionalCost: {}, Refund: {}, requestId: {}", 
                           feeInfo.getTotalAdditionalCost(), feeInfo.getTotalRefund(), requestId);
            }
        }
        
        return adminApprovalRequestMapper.findById(requestId);
    }
    
    /**
     * Process refund for reschedule operations
     */
    private void processRescheduleRefund(AdminApprovalRequest request, BigDecimal refundAmount) {
        try {
            logger.info("Processing reschedule refund for request {} with amount ¥{}", request.getId(), refundAmount);
            
            // Find the original payment for this ticket
            Payment payment = paymentMapper.findByTicketId(request.getTicketId());
            
            // Try to credit the user's account balance with the refund amount
            try {
                // Ensure the balance column exists
                ensureBalanceColumnExists();
                
                int balanceUpdated = userMapper.addToBalance(request.getUserId(), refundAmount);
                if (balanceUpdated > 0) {
                    logger.info("Successfully credited ¥{} to user {} balance for reschedule refund", 
                               refundAmount, request.getUserId());
                } else {
                    logger.error("Failed to credit balance for user {} - user may not exist", request.getUserId());
                }
            } catch (Exception balanceError) {
                logger.error("Failed to credit user balance (balance system may not be available): {}", balanceError.getMessage());
                logger.info("Proceeding with payment system refund only");
            }
            
            if (payment != null) {
                logger.info("Found payment record for ticket {}: payment number {}, trade no {}", 
                           request.getTicketId(), payment.getPaymentNumber(), payment.getAlipayTradeNo());
                
                // Try to process refund using payment number (works even if trade number is null)
                PaymentResponse refundResponse = alipayService.processRefundByTicketId(
                    request.getTicketId(), 
                    refundAmount, 
                    "改签退款差额 - 连接航班改签"
                );
                
                if (refundResponse.isSuccess()) {
                    logger.info("Payment system refund processed successfully for request {}", request.getId());
                    // Update request with refund transaction number
                    if (refundResponse.getRefundDetails() != null) {
                        String refundNo = (String) refundResponse.getRefundDetails().get("refundNo");
                        adminApprovalRequestMapper.updateRefundNumber(request.getId(), refundNo);
                    }
                } else {
                    logger.warn("Payment system refund processing failed for request {}: {}, but user balance has been credited", 
                               request.getId(), refundResponse.getMessage());
                    // The user balance has already been credited, which is the primary concern
                    logger.info("User will receive refund through account balance even though payment system refund failed");
                }
            } else {
                logger.warn("No payment record found for ticket {} - refund credited to user balance only", 
                           request.getTicketId());
                // User balance has been credited, which ensures they receive the refund
                logger.info("User will receive refund through account balance since no payment record exists");
            }
        } catch (Exception e) {
            logger.error("Failed to process reschedule refund for request " + request.getId(), e);
            // Try to credit user balance as fallback even if other processing fails
            try {
                ensureBalanceColumnExists();
                int balanceUpdated = userMapper.addToBalance(request.getUserId(), refundAmount);
                if (balanceUpdated > 0) {
                    logger.info("Fallback: Successfully credited ¥{} to user {} balance despite processing error", 
                               refundAmount, request.getUserId());
                }
            } catch (Exception balanceError) {
                logger.error("Critical: Failed to credit user balance as fallback for request " + request.getId(), balanceError);
            }
        }
    }
    
    /**
     * Ensure the balance column exists in the users table
     */
    private void ensureBalanceColumnExists() {
        try {
            userMapper.addBalanceColumn();
            logger.info("Added balance column to users table");
        } catch (Exception e) {
            // This is expected if column already exists
            logger.debug("Balance column already exists or could not be added: {}", e.getMessage());
        }
    }
    
    /**
     * Process rescheduling payment after admin approval
     */
    @Transactional
    public AdminApprovalRequest processReschedulePayment(Long requestId, String paymentNumber) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Approval request not found");
        }
        
        if (!"AWAITING_PAYMENT".equals(request.getStatus())) {
            throw new RuntimeException("Request is not awaiting payment");
        }
        
        if (!"PAYMENT_REQUIRED".equals(request.getPaymentStatus())) {
            throw new RuntimeException("Payment is not required for this request");
        }
        
        // Verify the payment was successful
        PaymentResponse paymentCheck = alipayService.queryPaymentStatus(paymentNumber);
        if (!paymentCheck.isSuccess() || !"SUCCESS".equals(paymentCheck.getStatus())) {
            throw new RuntimeException("Payment verification failed");
        }
        
        // Update request with payment info and complete status
        adminApprovalRequestMapper.updatePaymentNumber(requestId, paymentNumber);
        adminApprovalRequestMapper.updateStatus(
            requestId, "PAYMENT_COMPLETED", null, null, LocalDateTime.now()
        );
        
        // Payment completed - mark the new ticket as PAID and complete the reschedule
        // The ticket should already exist in PENDING_RESCHEDULE status from the approval stage
        // We need to update its status to PAID
        
        // Find the new ticket created during approval
        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
        if (originalTicket != null && originalTicket.getRescheduledToTicketId() != null) {
            Ticket newTicket = ticketService.getTicketById(originalTicket.getRescheduledToTicketId());
            if (newTicket != null && "PENDING_RESCHEDULE".equals(newTicket.getStatus())) {
                // Update the new ticket to PAID status
                ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
            }
        }
        
        return adminApprovalRequestMapper.findById(requestId);
    }
    
    @Transactional
    public AdminApprovalRequest rejectRequest(Long requestId, Long adminId, String rejectionReason) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is not pending");
        }
        
        // Update request status
        adminApprovalRequestMapper.updateStatus(
            requestId, "REJECTED", adminId, rejectionReason, LocalDateTime.now()
        );
        
        // Send rejection notification
        try {
            notificationService.createRescheduleRejectionNotification(request, rejectionReason);
        } catch (Exception e) {
            logger.error("Failed to send reschedule rejection notification for request " + requestId, e);
        }
        
        return adminApprovalRequestMapper.findById(requestId);
    }
    
    /**
     * Get user-friendly request type name for error messages
     */
    private String getRequestTypeName(String requestType) {
        switch (requestType) {
            case "REFUND":
                return "refund";
            case "CANCEL_AND_REFUND":
                return "cancellation and refund";
            case "RESCHEDULE":
                return "reschedule";
            default:
                return "change/refund";
        }
    }
    
    /**
     * Cancel a pending request
     */
    @Transactional
    public boolean cancelRequest(Long requestId, Long userId) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found");
        }
        
        // Verify the request belongs to the user
        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied - you can only cancel your own requests");
        }
        
        // Only allow canceling pending requests
        if (!"PENDING".equals(request.getStatus()) && !"AWAITING_PAYMENT".equals(request.getStatus())) {
            throw new RuntimeException("Only pending requests can be canceled");
        }
        
        // Update status to REJECTED with specific reason indicating user cancellation
        adminApprovalRequestMapper.updateStatus(
            requestId, "REJECTED", null, "Canceled by user", LocalDateTime.now()
        );
        
        logger.info("Request {} canceled by user {}", requestId, userId);
        return true;
    }
    
    /**
     * 直接更新审批请求状态 - 用于测试和故障恢复
     */
    @Transactional
    public void directUpdateApprovalStatus(Long requestId, String status, String paymentNumber) {
        try {
            // 尝试更新支付单号
            adminApprovalRequestMapper.updatePaymentNumber(requestId, paymentNumber);
        } catch (Exception e) {
            logger.warn("无法更新支付单号，可能是数据库字段不存在: {}", e.getMessage());
        }
        
        // 更新基本状态
        adminApprovalRequestMapper.updateStatus(
            requestId, status, null, null, LocalDateTime.now()
        );
        
        logger.info("直接更新审批请求状态完成: requestId={}, status={}, paymentNumber={}", 
                   requestId, status, paymentNumber);
    }
    
    /**
     * Fix existing PENDING_RESCHEDULE tickets that should have been processed with refunds
     */
    @Transactional
    public void fixPendingRescheduleTickets() {
        try {
            logger.info("Starting to fix PENDING_RESCHEDULE tickets that should have refunds processed");
            
            // Find all admin approval requests that resulted in PENDING_RESCHEDULE status
            // but should have had refunds processed
            List<AdminApprovalRequest> approvedRequests = adminApprovalRequestMapper.findByStatus("APPROVED");
            
            for (AdminApprovalRequest request : approvedRequests) {
                if ("RESCHEDULE".equals(request.getRequestType())) {
                    try {
                        // Check if there's a PENDING_RESCHEDULE ticket for this request
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        if (originalTicket != null && originalTicket.getRescheduledToTicketId() != null) {
                            Ticket newTicket = ticketService.getTicketById(originalTicket.getRescheduledToTicketId());
                            if (newTicket != null && "PENDING_RESCHEDULE".equals(newTicket.getStatus())) {
                                logger.info("Found PENDING_RESCHEDULE ticket that needs fixing: ticketId={}, requestId={}", 
                                           newTicket.getId(), request.getId());
                                
                                // Recalculate the fees to determine what should have happened
                                RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(request.getTicketId(), request.getNewFlightId());
                                
                                if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                                    // This should have been a refund case - fix it
                                    logger.info("Fixing ticket {} - should have refund of ¥{}", 
                                               newTicket.getId(), feeInfo.getTotalRefund());
                                    
                                    // Update ticket to PAID status
                                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                                    
                                    // Process the refund that was missed
                                    processRescheduleRefund(request, feeInfo.getTotalRefund());
                                    
                                    logger.info("Successfully fixed ticket {} with refund", newTicket.getId());
                                } else if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) == 0) {
                                    // No additional cost and no refund - just set to PAID
                                    logger.info("Fixing ticket {} - no payment required", newTicket.getId());
                                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                                    logger.info("Successfully fixed ticket {} - set to PAID", newTicket.getId());
                                } else {
                                    // Additional payment is required - this is correct, leave as PENDING_RESCHEDULE
                                    logger.info("Ticket {} correctly requires additional payment of ¥{}", 
                                               newTicket.getId(), feeInfo.getTotalAdditionalCost());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to fix request {}: {}", request.getId(), e.getMessage());
                    }
                }
            }
            
            logger.info("Completed fixing PENDING_RESCHEDULE tickets");
        } catch (Exception e) {
            logger.error("Error during PENDING_RESCHEDULE ticket fix process", e);
        }
    }

    /**
     * Enhanced validation method for connecting flight reschedules
     * This method now allows reschedule to any flight within the system, 
     * including individual segments of connecting flights
     */
    private boolean isValidRescheduleDestination(Ticket currentTicket, Long newFlightId) {
        try {
            // Get the new flight details
            Flight newFlight = flightService.getFlightById(newFlightId);
            if (newFlight == null) {
                logger.warn("New flight {} not found for reschedule validation", newFlightId);
                return false;
            }
            
            // Load the current ticket's connecting flights to determine the journey
            ticketService.loadConnectingFlights(currentTicket);
            Flight mainFlight = flightService.getFlightById(currentTicket.getFlightId());
            
            // Determine the origin and final destination of the original ticket
            Long originAirportId;
            Long finalDestinationAirportId;
            
            if (currentTicket.getConnectingFlights() != null && !currentTicket.getConnectingFlights().isEmpty()) {
                // For connecting flights
                originAirportId = mainFlight.getDepartureAirport().getId();
                Flight lastConnectingFlight = currentTicket.getConnectingFlights().get(currentTicket.getConnectingFlights().size() - 1);
                finalDestinationAirportId = lastConnectingFlight.getArrivalAirport().getId();
            } else {
                // For direct flights
                originAirportId = mainFlight.getDepartureAirport().getId();
                finalDestinationAirportId = mainFlight.getArrivalAirport().getId();
            }
            
            // Allow reschedule if the new flight:
            // 1. Starts from the same origin
            // 2. Goes to the same final destination
            // 3. Goes to any intermediate airport that can connect to final destination
            boolean isValidDestination = false;
            
            // Check if flight departs from original origin
            if (newFlight.getDepartureAirport().getId().equals(originAirportId)) {
                // Flight starts from original origin - this is always valid for reschedule
                isValidDestination = true;
                logger.debug("Origin match validation: New flight {} starts from original origin {}", 
                           newFlightId, originAirportId);
            }
            // Check if flight goes directly to final destination (from any departure point)
            else if (newFlight.getArrivalAirport().getId().equals(finalDestinationAirportId)) {
                // Flight goes to final destination - valid for connecting flight reschedule
                isValidDestination = true;
                logger.debug("Destination match validation: New flight {} goes to final destination {}", 
                           newFlightId, finalDestinationAirportId);
            }
            // For connecting flights, also allow intermediate segments
            else if (currentTicket.getConnectingFlights() != null && !currentTicket.getConnectingFlights().isEmpty()) {
                // Check if the new flight could be part of a valid connecting journey
                // This includes flights that go to intermediate airports within the original journey
                List<Flight> allOriginalFlights = new java.util.ArrayList<>();
                allOriginalFlights.add(mainFlight);
                allOriginalFlights.addAll(currentTicket.getConnectingFlights());
                
                // Check if new flight connects any two airports in the original journey
                for (Flight originalFlight : allOriginalFlights) {
                    if (newFlight.getDepartureAirport().getId().equals(originalFlight.getDepartureAirport().getId()) ||
                        newFlight.getArrivalAirport().getId().equals(originalFlight.getArrivalAirport().getId())) {
                        isValidDestination = true;
                        logger.debug("Connecting segment validation: New flight {} connects to original journey", newFlightId);
                        break;
                    }
                }
                
                // If still not valid, check if there are connecting flights available
                if (!isValidDestination) {
                    try {
                        List<Flight> connectingFlights = flightMapper.findAvailableFlightsForReschedule(
                            newFlight.getArrivalAirport().getId(),
                            finalDestinationAirportId,
                            null
                        );
                        
                        List<Flight> validConnectingFlights = connectingFlights.stream()
                            .filter(cf -> cf.getDepartureTimeUtc().isAfter(newFlight.getArrivalTimeUtc()))
                            .collect(Collectors.toList());
                        
                        if (!validConnectingFlights.isEmpty()) {
                            isValidDestination = true;
                            logger.debug("Connecting flight validation: New flight {} can connect to final destination {} via {} connecting flights", 
                                       newFlightId, finalDestinationAirportId, validConnectingFlights.size());
                        }
                    } catch (Exception e) {
                        logger.debug("Could not check connecting flights, but allowing reschedule: {}", e.getMessage());
                        // Be permissive - if we can't check connecting flights, allow the reschedule
                        // The user and admin can verify the logistics manually
                        isValidDestination = true;
                    }
                }
            }
            
            if (!isValidDestination) {
                logger.info("Reschedule validation failed for ticket {} to flight {}: Origin {} -> {}, New flight {} -> {}, Final destination {}", 
                          currentTicket.getId(), newFlightId, 
                          originAirportId, finalDestinationAirportId,
                          newFlight.getDepartureAirport().getId(), newFlight.getArrivalAirport().getId(),
                          finalDestinationAirportId);
            }
            
            return isValidDestination;
            
        } catch (Exception e) {
            logger.error("Error validating reschedule destination for ticket {} to flight {}: {}", 
                        currentTicket.getId(), newFlightId, e.getMessage(), e);
            // Be permissive on errors - allow reschedule and let admin verify manually
            return true;
        }
    }
}