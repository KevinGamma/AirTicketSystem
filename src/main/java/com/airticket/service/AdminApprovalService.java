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

        if (!"PAID".equals(ticket.getStatus()) && !"BOOKED".equals(ticket.getStatus())) {
            throw new RuntimeException("Only paid or booked tickets can be refunded/cancelled");
        }

        List<AdminApprovalRequest> pendingRequests = adminApprovalRequestMapper.findPendingRequestsByTicketId(ticketId);
        if (!pendingRequests.isEmpty()) {
            AdminApprovalRequest existingRequest = pendingRequests.get(0);
            String requestTypeName = getRequestTypeName(existingRequest.getRequestType());
            throw new RuntimeException("A " + requestTypeName + " request is already pending for this ticket. " +
                    "Please wait for the current request to be processed or cancel it before submitting a new request.");
        }

        BigDecimal serviceFee = ticketService.calculateRefundServiceFee(ticket);

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

        List<AdminApprovalRequest> pendingRequests = adminApprovalRequestMapper.findPendingRequestsByTicketId(ticketId);
        if (!pendingRequests.isEmpty()) {
            AdminApprovalRequest existingRequest = pendingRequests.get(0);
            String requestTypeName = getRequestTypeName(existingRequest.getRequestType());
            throw new RuntimeException("A " + requestTypeName + " request is already pending for this ticket. " +
                    "Please wait for the current request to be processed or cancel it before submitting a new request.");
        }

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
            adminApprovalRequestMapper.updateStatus(
                    requestId, "APPROVED", adminId, null, LocalDateTime.now()
            );
            ticketService.refundTicket(request.getTicketId(), request.getReason());

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
            RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(request.getTicketId(), request.getNewFlightId());

            try {
                if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                            requestId, "AWAITING_PAYMENT", adminId, null, LocalDateTime.now(),
                            feeInfo.getTotalAdditionalCost(), null, "PAYMENT_REQUIRED"
                    );
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), feeInfo.getTotalAdditionalCost());

                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        request.setAdditionalFee(feeInfo.getTotalAdditionalCost());
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                } else if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                            requestId, "APPROVED", adminId, null, LocalDateTime.now(),
                            null, feeInfo.getTotalRefund(), "REFUND_REQUIRED"
                    );

                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                    processRescheduleRefund(request, feeInfo.getTotalRefund());

                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        request.setRefundAmount(feeInfo.getTotalRefund());
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);

                        notificationService.scheduleFlightReminder(newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                } else {
                    adminApprovalRequestMapper.updateStatusWithPaymentInfo(
                            requestId, "APPROVED", adminId, null, LocalDateTime.now(),
                            null, null, "NOT_REQUIRED"
                    );
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());

                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        notificationService.createRescheduleApprovalNotification(request, originalTicket, newTicket);

                        notificationService.scheduleFlightReminder(newTicket);
                    } catch (Exception e) {
                        logger.error("Failed to send reschedule approval notification for request " + requestId, e);
                    }
                }
            } catch (Exception e) {
                logger.error("Database not updated with payment fields, falling back to pending status approach", e);
                adminApprovalRequestMapper.updateStatus(
                        requestId, "APPROVED", adminId, null, LocalDateTime.now()
                );

                if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                    processRescheduleRefund(request, feeInfo.getTotalRefund());
                } else if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
                    ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), feeInfo.getTotalAdditionalCost());
                } else {
                    Ticket newTicket = ticketService.rescheduleTicketWithPendingStatus(request.getTicketId(), request.getNewFlightId(), request.getReason(), BigDecimal.ZERO);
                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                }

                logger.info("Reschedule approved with fee info - AdditionalCost: {}, Refund: {}, requestId: {}",
                        feeInfo.getTotalAdditionalCost(), feeInfo.getTotalRefund(), requestId);
            }
        }

        return adminApprovalRequestMapper.findById(requestId);
    }

    private void processRescheduleRefund(AdminApprovalRequest request, BigDecimal refundAmount) {
        try {
            logger.info("Processing reschedule refund for request {} with amount ¥{}", request.getId(), refundAmount);

            Payment payment = paymentMapper.findByTicketId(request.getTicketId());

            try {
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

                PaymentResponse refundResponse = alipayService.processRefundByTicketId(
                        request.getTicketId(),
                        refundAmount,
                        "改签退款差额 - 连接航班改签"
                );

                if (refundResponse.isSuccess()) {
                    logger.info("Payment system refund processed successfully for request {}", request.getId());
                    if (refundResponse.getRefundDetails() != null) {
                        String refundNo = (String) refundResponse.getRefundDetails().get("refundNo");
                        adminApprovalRequestMapper.updateRefundNumber(request.getId(), refundNo);
                    }
                } else {
                    logger.warn("Payment system refund processing failed for request {}: {}, but user balance has been credited",
                            request.getId(), refundResponse.getMessage());
                    logger.info("User will receive refund through account balance even though payment system refund failed");
                }
            } else {
                logger.warn("No payment record found for ticket {} - refund credited to user balance only",
                        request.getTicketId());
                logger.info("User will receive refund through account balance since no payment record exists");
            }
        } catch (Exception e) {
            logger.error("Failed to process reschedule refund for request " + request.getId(), e);
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

    private void ensureBalanceColumnExists() {
        try {
            userMapper.addBalanceColumn();
            logger.info("Added balance column to users table");
        } catch (Exception e) {
            logger.debug("Balance column already exists or could not be added: {}", e.getMessage());
        }
    }

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

        PaymentResponse paymentCheck = alipayService.queryPaymentStatus(paymentNumber);
        if (!paymentCheck.isSuccess() || !"SUCCESS".equals(paymentCheck.getStatus())) {
            throw new RuntimeException("Payment verification failed");
        }

        adminApprovalRequestMapper.updatePaymentNumber(requestId, paymentNumber);
        adminApprovalRequestMapper.updateStatus(
                requestId, "PAYMENT_COMPLETED", null, null, LocalDateTime.now()
        );

        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
        if (originalTicket != null && originalTicket.getRescheduledToTicketId() != null) {
            Ticket newTicket = ticketService.getTicketById(originalTicket.getRescheduledToTicketId());
            if (newTicket != null && "PENDING_RESCHEDULE".equals(newTicket.getStatus())) {
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

        adminApprovalRequestMapper.updateStatus(
                requestId, "REJECTED", adminId, rejectionReason, LocalDateTime.now()
        );

        try {
            notificationService.createRescheduleRejectionNotification(request, rejectionReason);
        } catch (Exception e) {
            logger.error("Failed to send reschedule rejection notification for request " + requestId, e);
        }

        return adminApprovalRequestMapper.findById(requestId);
    }

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

    @Transactional
    public boolean cancelRequest(Long requestId, Long userId) {
        AdminApprovalRequest request = adminApprovalRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found");
        }

        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied - you can only cancel your own requests");
        }

        if (!"PENDING".equals(request.getStatus()) && !"AWAITING_PAYMENT".equals(request.getStatus())) {
            throw new RuntimeException("Only pending requests can be canceled");
        }

        adminApprovalRequestMapper.updateStatus(
                requestId, "REJECTED", null, "Canceled by user", LocalDateTime.now()
        );

        logger.info("Request {} canceled by user {}", requestId, userId);
        return true;
    }

    @Transactional
    public void directUpdateApprovalStatus(Long requestId, String status, String paymentNumber) {
        try {
            adminApprovalRequestMapper.updatePaymentNumber(requestId, paymentNumber);
        } catch (Exception e) {
            logger.warn("无法更新支付单号，可能是数据库字段不存在: {}", e.getMessage());
        }

        adminApprovalRequestMapper.updateStatus(
                requestId, status, null, null, LocalDateTime.now()
        );

        logger.info("直接更新审批请求状态完成: requestId={}, status={}, paymentNumber={}",
                requestId, status, paymentNumber);
    }

    @Transactional
    public void fixPendingRescheduleTickets() {
        try {
            logger.info("Starting to fix PENDING_RESCHEDULE tickets that should have refunds processed");

            List<AdminApprovalRequest> approvedRequests = adminApprovalRequestMapper.findByStatus("APPROVED");

            for (AdminApprovalRequest request : approvedRequests) {
                if ("RESCHEDULE".equals(request.getRequestType())) {
                    try {
                        Ticket originalTicket = ticketService.getTicketById(request.getTicketId());
                        if (originalTicket != null && originalTicket.getRescheduledToTicketId() != null) {
                            Ticket newTicket = ticketService.getTicketById(originalTicket.getRescheduledToTicketId());
                            if (newTicket != null && "PENDING_RESCHEDULE".equals(newTicket.getStatus())) {
                                logger.info("Found PENDING_RESCHEDULE ticket that needs fixing: ticketId={}, requestId={}",
                                        newTicket.getId(), request.getId());

                                RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(request.getTicketId(), request.getNewFlightId());

                                if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
                                    logger.info("Fixing ticket {} - should have refund of ¥{}",
                                            newTicket.getId(), feeInfo.getTotalRefund());

                                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());

                                    processRescheduleRefund(request, feeInfo.getTotalRefund());

                                    logger.info("Successfully fixed ticket {} with refund", newTicket.getId());
                                } else if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) == 0) {
                                    logger.info("Fixing ticket {} - no payment required", newTicket.getId());
                                    ticketService.updateTicketStatus(newTicket.getId(), "PAID", Instant.now());
                                    logger.info("Successfully fixed ticket {} - set to PAID", newTicket.getId());
                                } else {
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

    private boolean isValidRescheduleDestination(Ticket currentTicket, Long newFlightId) {
        try {
            Flight newFlight = flightService.getFlightById(newFlightId);
            if (newFlight == null) {
                logger.warn("New flight {} not found for reschedule validation", newFlightId);
                return false;
            }

            ticketService.loadConnectingFlights(currentTicket);
            Flight mainFlight = flightService.getFlightById(currentTicket.getFlightId());

            Long originAirportId;
            Long finalDestinationAirportId;

            if (currentTicket.getConnectingFlights() != null && !currentTicket.getConnectingFlights().isEmpty()) {
                originAirportId = mainFlight.getDepartureAirport().getId();
                Flight lastConnectingFlight = currentTicket.getConnectingFlights().get(currentTicket.getConnectingFlights().size() - 1);
                finalDestinationAirportId = lastConnectingFlight.getArrivalAirport().getId();
            } else {
                originAirportId = mainFlight.getDepartureAirport().getId();
                finalDestinationAirportId = mainFlight.getArrivalAirport().getId();
            }

            boolean isValidDestination = false;

            if (newFlight.getDepartureAirport().getId().equals(originAirportId)) {
                isValidDestination = true;
                logger.debug("Origin match validation: New flight {} starts from original origin {}",
                        newFlightId, originAirportId);
            }
            else if (newFlight.getArrivalAirport().getId().equals(finalDestinationAirportId)) {
                isValidDestination = true;
                logger.debug("Destination match validation: New flight {} goes to final destination {}",
                        newFlightId, finalDestinationAirportId);
            }
            else if (currentTicket.getConnectingFlights() != null && !currentTicket.getConnectingFlights().isEmpty()) {
                List<Flight> allOriginalFlights = new java.util.ArrayList<>();
                allOriginalFlights.add(mainFlight);
                allOriginalFlights.addAll(currentTicket.getConnectingFlights());

                for (Flight originalFlight : allOriginalFlights) {
                    if (newFlight.getDepartureAirport().getId().equals(originalFlight.getDepartureAirport().getId()) ||
                            newFlight.getArrivalAirport().getId().equals(originalFlight.getArrivalAirport().getId())) {
                        isValidDestination = true;
                        logger.debug("Connecting segment validation: New flight {} connects to original journey", newFlightId);
                        break;
                    }
                }

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
            return true;
        }
    }
}