package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.dto.BookingRequest;
import com.airticket.dto.PaymentRequest;
import com.airticket.dto.PaymentResponse;
import com.airticket.dto.RefundRequest;
import com.airticket.dto.RescheduleRequest;
import com.airticket.dto.RescheduleFeeInfo;
import com.airticket.model.AdminApprovalRequest;
import com.airticket.model.Flight;
import com.airticket.model.Ticket;
import com.airticket.model.User;
import com.airticket.service.AdminApprovalService;
import com.airticket.service.AlipayService;
import com.airticket.service.FlightService;
import com.airticket.service.ReschedulePaymentMonitorService;
import com.airticket.service.TicketService;
import com.airticket.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminApprovalService adminApprovalService;

    @Autowired
    private AlipayService alipayService;
    
    @Autowired
    private FlightService flightService;
    
    @Autowired
    private ReschedulePaymentMonitorService reschedulePaymentMonitorService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Ticket>>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Ticket>>> getMyTickets() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.findByUsername(username);
        
        List<Ticket> tickets = ticketService.getTicketsByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> getTicketById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.findByUsername(username);
        
        Ticket ticket = ticketService.getTicketById(id);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<ApiResponse<Ticket>> getTicketByNumber(@PathVariable String ticketNumber) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.findByUsername(username);
        
        Ticket ticket = ticketService.findByTicketNumber(ticketNumber);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Ticket>> bookTicket(@Valid @RequestBody BookingRequest request) {
        try {
            logger.info("=== BOOKING REQUEST RECEIVED ===");
            logger.info("Flight ID: {}", request.getFlightId());
            logger.info("Passenger Name: {}", request.getPassengerName());
            logger.info("Passenger ID Number: {}", request.getPassengerIdNumber());
            logger.info("Ticket Type: {}", request.getTicketType());
            logger.info("Seat Number: {}", request.getSeatNumber());
            logger.info("Connecting Flight IDs: {}", request.getConnectingFlightIds());
            
            if (request.getConnectingFlightIds() != null && !request.getConnectingFlightIds().isEmpty()) {
                logger.info("*** THIS IS A CONNECTING FLIGHT BOOKING ***");
                logger.info("Number of connecting flights: {}", request.getConnectingFlightIds().size());
                for (int i = 0; i < request.getConnectingFlightIds().size(); i++) {
                    logger.info("Connecting flight {}: ID = {}", i + 1, request.getConnectingFlightIds().get(i));
                }
            } else {
                logger.info("*** THIS IS A DIRECT FLIGHT BOOKING ***");
            }
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            logger.info("User booking ticket: ID = {}, Username = {}", user.getId(), username);
            
            logger.info("Calling ticketService.bookTicket()...");
            Ticket ticket = ticketService.bookTicket(user.getId(), request);
            logger.info("Ticket booking successful! Ticket ID: {}, Ticket Number: {}", ticket.getId(), ticket.getTicketNumber());
            
            if (ticket.getConnectingFlights() != null && !ticket.getConnectingFlights().isEmpty()) {
                logger.info("Connecting flights loaded in response:");
                for (int i = 0; i < ticket.getConnectingFlights().size(); i++) {
                    logger.info("Connecting flight {}: ID = {}, Flight Number = {}", 
                               i + 1, ticket.getConnectingFlights().get(i).getId(), 
                               ticket.getConnectingFlights().get(i).getFlightNumber());
                }
            } else {
                logger.info("No connecting flights in response");
            }
            
            String message = "Ticket booked successfully! IMPORTANT: You must complete payment within 10 minutes of booking, or your reservation will expire.";
            return ResponseEntity.ok(ApiResponse.success(message, ticket));
        } catch (Exception e) {
            logger.error("=== BOOKING ERROR ===");
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Full stack trace:", e);
            System.err.println("error occurred when booking a ticket: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to book ticket: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/payment-deadline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentDeadline(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Ticket not found"));
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            Map<String, Object> paymentInfo = ticketService.getPaymentDeadlineInfo(id);
            return ResponseEntity.ok(ApiResponse.success(paymentInfo));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<Ticket>> payTicket(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ticket not found"));
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            Ticket paidTicket = ticketService.payTicket(id);
            return ResponseEntity.ok(ApiResponse.success("Ticket paid successfully", paidTicket));
        } catch (Exception e) {
            e.printStackTrace(); 
            System.err.println("Payment error for ticket " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to pay ticket: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/change")
    public ResponseEntity<ApiResponse<Ticket>> changeTicket(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            Long newFlightId = Long.valueOf(request.get("newFlightId").toString());
            String reason = (String) request.get("reason");
            
            Ticket changedTicket = ticketService.changeTicket(id, newFlightId, reason);
            return ResponseEntity.ok(ApiResponse.success("Ticket changed successfully", changedTicket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to change ticket: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTicket(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            String reason = request != null ? request.get("reason") : null;
            ticketService.cancelTicket(id, reason);
            
            return ResponseEntity.ok(ApiResponse.success("Ticket cancelled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to cancel ticket: " + e.getMessage()));
        }
    }

    


    @PostMapping("/{id}/refund-request")
    public ResponseEntity<ApiResponse<AdminApprovalRequest>> requestRefund(@PathVariable Long id, @RequestBody RefundRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            AdminApprovalRequest approvalRequest = adminApprovalService.createRefundRequest(
                id, user.getId(), request.getReason()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Refund request submitted successfully. Please wait for admin approval.", approvalRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit refund request: " + e.getMessage()));
        }
    }

    


    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processDirectRefund(@PathVariable Long id, @RequestBody RefundRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            
            Map<String, Object> refundInfo = ticketService.processDirectRefund(id, request.getReason(), user.getId());
            
            return ResponseEntity.ok(ApiResponse.success("Ticket refunded successfully", refundInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process refund: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/available-flights")
    public ResponseEntity<ApiResponse<List<Flight>>> getAvailableFlightsForReschedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false, defaultValue = "true") Boolean includeConnecting) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            List<Flight> availableFlights;
            if (date != null || !includeConnecting) {
                availableFlights = ticketService.getAvailableFlightsForReschedule(id, date, includeConnecting);
            } else {
                availableFlights = ticketService.getAvailableFlightsForReschedule(id);
            }
            return ResponseEntity.ok(ApiResponse.success(availableFlights));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get available flights: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<AdminApprovalRequest>> requestReschedule(@PathVariable Long id, @RequestBody RescheduleRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            AdminApprovalRequest approvalRequest = adminApprovalService.createRescheduleRequest(
                id, user.getId(), request.getNewFlightId(), request.getReason()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Reschedule request submitted successfully. Please wait for admin approval.", approvalRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit reschedule request: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/reschedule-fee")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateRescheduleServiceFee(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            BigDecimal serviceFee = ticketService.calculateRescheduleServiceFee(ticket);
            return ResponseEntity.ok(ApiResponse.success(serviceFee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to calculate service fee: " + e.getMessage()));
        }
    }

    



    @GetMapping("/{id}/reschedule-fee-info/{newFlightId}")
    public ResponseEntity<ApiResponse<RescheduleFeeInfo>> getRescheduleFeeInfo(
            @PathVariable Long id, 
            @PathVariable Long newFlightId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(id, newFlightId);
            return ResponseEntity.ok(ApiResponse.success("Reschedule fee information calculated successfully", feeInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to calculate reschedule fee information: " + e.getMessage()));
        }
    }
    
    





    @PostMapping("/{id}/compare-reschedule-fees")
    public ResponseEntity<ApiResponse<List<RescheduleFeeInfo>>> compareRescheduleFees(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            List<Long> flightIds = request.get("flightIds");
            if (flightIds == null || flightIds.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Flight IDs are required"));
            }
            
            List<RescheduleFeeInfo> feeInfoList = new ArrayList<>();
            for (Long flightId : flightIds) {
                try {
                    RescheduleFeeInfo feeInfo = ticketService.calculateRescheduleFeeInfo(id, flightId);
                    feeInfoList.add(feeInfo);
                } catch (Exception e) {
                    
                    continue;
                }
            }
            
            
            feeInfoList.sort((a, b) -> {
                BigDecimal costA = a.getTotalAdditionalCost().subtract(a.getTotalRefund());
                BigDecimal costB = b.getTotalAdditionalCost().subtract(b.getTotalRefund());
                return costA.compareTo(costB);
            });
            
            return ResponseEntity.ok(ApiResponse.success("Reschedule fee comparison completed", feeInfoList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to compare reschedule fees: " + e.getMessage()));
        }
    }
    
    



    @PostMapping("/{id}/reschedule-payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> createReschedulePayment(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            
            if (!"PENDING_RESCHEDULE".equals(ticket.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("This ticket does not require reschedule payment"));
            }
            
            
            Long originalTicketId = findOriginalTicketIdFromReschedule(id);
            if (originalTicketId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unable to find original ticket for this reschedule"));
            }
            
            
            List<AdminApprovalRequest> allRequests = adminApprovalService.getRequestsByUserId(user.getId());
            logger.info("Debug - User {} has {} approval requests for originalTicketId {}", user.getId(), allRequests.size(), originalTicketId);
            for (AdminApprovalRequest req : allRequests) {
                logger.info("Debug - Request ID: {}, Type: {}, Status: {}, TicketId: {}", 
                           req.getId(), req.getRequestType(), req.getStatus(), req.getTicketId());
            }
            
            
            AdminApprovalRequest request = allRequests.stream()
                .filter(r -> "RESCHEDULE".equals(r.getRequestType()) 
                          && ("AWAITING_PAYMENT".equals(r.getStatus()) || "APPROVED".equals(r.getStatus()))
                          && r.getTicketId().equals(originalTicketId))
                .findFirst()
                .orElse(null);
                
            if (request == null) {
                logger.error("Debug - No matching request found for original ticket ID: {}", originalTicketId);
                return ResponseEntity.badRequest().body(ApiResponse.error("No pending reschedule payment found for this ticket"));
            }
            
            
            BigDecimal paymentAmount = request.getTotalAmount();
            
            
            if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.info("Debug - TotalAmount not available in request, calculating from fee info");
                try {
                    RescheduleFeeInfo feeInfo = adminApprovalService.getApprovalRequestFeeInfo(request.getId());
                    paymentAmount = feeInfo.getTotalAdditionalCost();
                    logger.info("Debug - Calculated payment amount: {}", paymentAmount);
                } catch (Exception e) {
                    logger.error("Debug - Failed to calculate fee info: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(ApiResponse.error("Unable to calculate payment amount for this reschedule"));
                }
            }
            
            if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No payment amount found - this reschedule may not require additional payment"));
            }
            
            
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setTicketId(id);
            paymentRequest.setAmount(paymentAmount);
            paymentRequest.setPaymentMethod("ALIPAY");
            paymentRequest.setUseSandbox(true);
            paymentRequest.setReturnUrl("http://localhost:3000/tickets/" + id + "/reschedule-success");
            
            logger.info("Debug - Creating payment for amount: {}", paymentAmount);
            PaymentResponse paymentResponse = alipayService.createPayment(paymentRequest);
            
            if (paymentResponse.isSuccess()) {
                
                paymentResponse.setMessage("改签费用支付订单创建成功，请完成支付以完成改签操作。支付金额：¥" + paymentAmount);
                return ResponseEntity.ok(ApiResponse.success("Reschedule payment order created successfully", paymentResponse));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create reschedule payment: " + paymentResponse.getMessage()));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create reschedule payment: " + e.getMessage()));
        }
    }
    
    



    @PostMapping("/{id}/complete-reschedule/{paymentNumber}")
    public ResponseEntity<ApiResponse<AdminApprovalRequest>> completeReschedulePayment(
            @PathVariable Long id, 
            @PathVariable String paymentNumber) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            
            if (!"PENDING_RESCHEDULE".equals(ticket.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("This ticket does not require reschedule payment"));
            }
            
            
            Long originalTicketId = findOriginalTicketIdFromReschedule(id);
            if (originalTicketId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unable to find original ticket for this reschedule"));
            }
            
            
            AdminApprovalRequest request = adminApprovalService.getRequestsByUserId(user.getId()).stream()
                .filter(r -> "RESCHEDULE".equals(r.getRequestType()) 
                          && ("AWAITING_PAYMENT".equals(r.getStatus()) || "APPROVED".equals(r.getStatus()))
                          && r.getTicketId().equals(originalTicketId))
                .findFirst()
                .orElse(null);
                
            if (request == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No pending reschedule payment found"));
            }
            
            
            AdminApprovalRequest completedRequest = adminApprovalService.processReschedulePayment(request.getId(), paymentNumber);
            
            return ResponseEntity.ok(ApiResponse.success("Reschedule payment completed successfully! Your flight has been rescheduled.", completedRequest));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to complete reschedule payment: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/refund-fee")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateRefundServiceFee(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            BigDecimal serviceFee = ticketService.calculateRefundServiceFee(ticket);
            return ResponseEntity.ok(ApiResponse.success(serviceFee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to calculate service fee: " + e.getMessage()));
        }
    }

    


    @GetMapping("/{id}/refund-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRefundInfo(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            
            if (!"PAID".equals(ticket.getStatus()) && !"BOOKED".equals(ticket.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Only paid or booked tickets can be refunded"));
            }
            
            if ("CANCELLED".equals(ticket.getStatus()) || "REFUNDED".equals(ticket.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ticket is already cancelled or refunded"));
            }
            
            
            Flight flight = flightService.getFlightById(ticket.getFlightId());
            boolean canRefund = true;
            String timeWarning = null;
            
            if (flight != null) {
                Instant now = Instant.now();
                if (now.isAfter(flight.getDepartureTimeUtc().minusSeconds(2 * 60 * 60))) {
                    canRefund = false;
                    timeWarning = "Cannot refund tickets within 2 hours of departure time";
                }
            }
            
            BigDecimal serviceFee = ticketService.calculateRefundServiceFee(ticket);
            BigDecimal refundAmount = "PAID".equals(ticket.getStatus()) ? 
                ticket.getPrice().subtract(serviceFee) : BigDecimal.ZERO;
            
            Map<String, Object> refundInfo = new HashMap<>();
            refundInfo.put("canRefund", canRefund);
            refundInfo.put("timeWarning", timeWarning);
            refundInfo.put("ticketId", id);
            refundInfo.put("ticketStatus", ticket.getStatus());
            refundInfo.put("originalAmount", ticket.getPrice());
            refundInfo.put("serviceFee", serviceFee);
            refundInfo.put("refundAmount", refundAmount);
            refundInfo.put("isPaid", "PAID".equals(ticket.getStatus()));
            
            return ResponseEntity.ok(ApiResponse.success("Refund information retrieved", refundInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get refund info: " + e.getMessage()));
        }
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        try {
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            ticketService.hardDeleteTicket(id);
            return ResponseEntity.ok(ApiResponse.success("Ticket deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete ticket: " + e.getMessage()));
        }
    }

    
    @PostMapping("/{id}/delete")
    public ResponseEntity<ApiResponse<Void>> softDeleteTicket(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            ticketService.softDeleteTicket(id);
            return ResponseEntity.ok(ApiResponse.success("Ticket hidden successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to hide ticket: " + e.getMessage()));
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<AdminApprovalRequest>>> getMyRequests() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            List<AdminApprovalRequest> requests = adminApprovalService.getRequestsByUserId(user.getId());
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get requests: " + e.getMessage()));
        }
    }
    
    



    @PostMapping("/{id}/complete-reschedule-payment-test")
    public ResponseEntity<ApiResponse<String>> completeReschedulePaymentTest(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            if (!"PENDING_RESCHEDULE".equals(ticket.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ticket is not in PENDING_RESCHEDULE status"));
            }
            
            
            ticketService.updateTicketStatus(id, "PAID", Instant.now());
            
            
            Long originalTicketId = ticket.getOriginalTicketId();
            if (originalTicketId != null) {
                List<AdminApprovalRequest> requests = adminApprovalService.getRequestsByUserId(user.getId());
                AdminApprovalRequest rescheduleRequest = requests.stream()
                    .filter(r -> "RESCHEDULE".equals(r.getRequestType()) 
                              && ("AWAITING_PAYMENT".equals(r.getStatus()) || "APPROVED".equals(r.getStatus()))
                              && r.getTicketId().equals(originalTicketId))
                    .findFirst()
                    .orElse(null);
                    
                if (rescheduleRequest != null) {
                    String testPaymentNumber = "TEST_PAYMENT_" + System.currentTimeMillis();
                    logger.info("Test completion: Found reschedule request ID: {}, current status: {}", 
                               rescheduleRequest.getId(), rescheduleRequest.getStatus());
                    
                    try {
                        
                        AdminApprovalRequest completedRequest = adminApprovalService.processReschedulePayment(
                            rescheduleRequest.getId(), testPaymentNumber);
                        
                        logger.info("Test completion: Request status after processing: {}", completedRequest.getStatus());
                        
                    } catch (Exception e) {
                        logger.error("Failed to process reschedule payment via service: {}", e.getMessage(), e);
                        
                        
                        try {
                            logger.info("Using direct database update fallback");
                            
                            
                            adminApprovalService.directUpdateApprovalStatus(
                                rescheduleRequest.getId(), 
                                "PAYMENT_COMPLETED", 
                                testPaymentNumber
                            );
                            
                            logger.info("Direct database update completed successfully");
                            
                        } catch (Exception e2) {
                            logger.error("Direct database update also failed: {}", e2.getMessage(), e2);
                        }
                    }
                } else {
                    logger.warn("Test completion: No reschedule request found for originalTicketId: {}", originalTicketId);
                }
            } else {
                logger.warn("Test completion: No originalTicketId found for ticket: {}", ticket.getId());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Reschedule payment completed manually for testing", "Payment completed successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to complete reschedule payment test", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to complete payment: " + e.getMessage()));
        }
    }
    
    



    @GetMapping("/{id}/debug-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugTicketStatus(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            Map<String, Object> debugInfo = new HashMap<>();
            
            
            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("id", ticket.getId());
            ticketInfo.put("ticketNumber", ticket.getTicketNumber());
            ticketInfo.put("status", ticket.getStatus());
            ticketInfo.put("originalTicketId", ticket.getOriginalTicketId());
            ticketInfo.put("rescheduledToTicketId", ticket.getRescheduledToTicketId());
            ticketInfo.put("isOriginalTicket", ticket.getIsOriginalTicket());
            debugInfo.put("ticket", ticketInfo);
            
            
            List<AdminApprovalRequest> allRequests = new ArrayList<>();
            
            
            if (ticket.getOriginalTicketId() != null) {
                List<AdminApprovalRequest> originalRequests = adminApprovalService.getRequestsByUserId(user.getId()).stream()
                    .filter(r -> r.getTicketId().equals(ticket.getOriginalTicketId()))
                    .collect(java.util.stream.Collectors.toList());
                allRequests.addAll(originalRequests);
            }
            
            
            List<AdminApprovalRequest> currentRequests = adminApprovalService.getRequestsByUserId(user.getId()).stream()
                .filter(r -> r.getTicketId().equals(ticket.getId()))
                .collect(java.util.stream.Collectors.toList());
            allRequests.addAll(currentRequests);
            
            List<Map<String, Object>> requestsInfo = new ArrayList<>();
            for (AdminApprovalRequest request : allRequests) {
                Map<String, Object> reqInfo = new HashMap<>();
                reqInfo.put("id", request.getId());
                reqInfo.put("ticketId", request.getTicketId());
                reqInfo.put("requestType", request.getRequestType());
                reqInfo.put("status", request.getStatus());
                reqInfo.put("totalAmount", request.getTotalAmount());
                reqInfo.put("refundAmount", request.getRefundAmount());
                reqInfo.put("paymentStatus", request.getPaymentStatus());
                reqInfo.put("paymentNumber", request.getPaymentNumber());
                requestsInfo.add(reqInfo);
            }
            debugInfo.put("approvalRequests", requestsInfo);
            
            
            Map<String, Object> frontendLogic = new HashMap<>();
            frontendLogic.put("canPayRescheduleFee", "PENDING_RESCHEDULE".equals(ticket.getStatus()));
            frontendLogic.put("canPay", "BOOKED".equals(ticket.getStatus()));
            frontendLogic.put("canReschedule", "PAID".equals(ticket.getStatus()));
            debugInfo.put("frontendButtonLogic", frontendLogic);
            
            return ResponseEntity.ok(ApiResponse.success("Debug information retrieved", debugInfo));
            
        } catch (Exception e) {
            logger.error("Failed to get debug status", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get debug info: " + e.getMessage()));
        }
    }
    
    



    @PostMapping("/trigger-payment-monitoring")
    public ResponseEntity<ApiResponse<String>> triggerPaymentMonitoring() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            
            logger.info("Manual payment monitoring triggered by user: {}", username);
            
            
            reschedulePaymentMonitorService.runPaymentMonitoring();
            
            return ResponseEntity.ok(ApiResponse.success("Payment monitoring triggered successfully", "Monitoring completed"));
            
        } catch (Exception e) {
            logger.error("Failed to trigger payment monitoring", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to trigger monitoring: " + e.getMessage()));
        }
    }
    
    


    private boolean isNewTicketFromReschedule(Long newTicketId, Long originalTicketId) {
        try {
            Ticket originalTicket = ticketService.getTicketById(originalTicketId);
            return originalTicket != null && 
                   originalTicket.getRescheduledToTicketId() != null && 
                   originalTicket.getRescheduledToTicketId().equals(newTicketId);
        } catch (Exception e) {
            return false;
        }
    }
    
    


    private Long findOriginalTicketIdFromReschedule(Long newTicketId) {
        try {
            Ticket newTicket = ticketService.getTicketById(newTicketId);
            return newTicket != null ? newTicket.getOriginalTicketId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    



    @GetMapping("/{id}/pending-requests")
    public ResponseEntity<ApiResponse<List<AdminApprovalRequest>>> getPendingRequests(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Ticket ticket = ticketService.getTicketById(id);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!user.getRole().equals("ADMIN") && !ticket.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            
            List<AdminApprovalRequest> pendingRequests = adminApprovalService.getPendingRequestsByTicketId(id);
            return ResponseEntity.ok(ApiResponse.success(pendingRequests));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get pending requests: " + e.getMessage()));
        }
    }
    
    



    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<String>> cancelRequest(@PathVariable Long requestId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            adminApprovalService.cancelRequest(requestId, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Request canceled successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to cancel request: " + e.getMessage()));
        }
    }
    
    


    @GetMapping("/debug/timezone-analysis")
    public ResponseEntity<Map<String, Object>> getTimezoneDebugInfo() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            Map<String, Object> debugInfo = new HashMap<>();
            
            
            debugInfo.put("systemTimezone", java.time.ZoneId.systemDefault().toString());
            debugInfo.put("currentTime", java.time.Instant.now().toString());
            debugInfo.put("currentTimeFormatted", java.time.LocalDateTime.now().toString());
            
            
            List<Ticket> tickets = ticketService.getTicketsByUserId(user.getId());
            List<Map<String, Object>> ticketAnalysis = new ArrayList<>();
            
            for (Ticket ticket : tickets) {
                if (ticket.getFlight() != null) {
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("ticketId", ticket.getId());
                    analysis.put("ticketNumber", ticket.getTicketNumber());
                    analysis.put("flightNumber", ticket.getFlight().getFlightNumber());
                    
                    
                    analysis.put("rawDepartureTimeUtc", ticket.getFlight().getDepartureTimeUtc());
                    analysis.put("rawArrivalTimeUtc", ticket.getFlight().getArrivalTimeUtc());
                    
                    
                    analysis.put("jsonDepartureTime", ticket.getFlight().getDepartureTime());
                    analysis.put("jsonArrivalTime", ticket.getFlight().getArrivalTime());
                    
                    
                    if (ticket.getFlight().getDepartureTimeUtc() != null) {
                        try {
                            java.time.ZoneId shanghaiZone = java.time.ZoneId.of("Asia/Shanghai");
                            java.time.ZoneId utcZone = java.time.ZoneId.of("UTC");
                            
                            java.time.LocalDateTime depInShanghai = ticket.getFlight().getDepartureTimeUtc()
                                .atZone(utcZone).withZoneSameInstant(shanghaiZone).toLocalDateTime();
                            java.time.LocalDateTime arrInShanghai = ticket.getFlight().getArrivalTimeUtc() != null ?
                                ticket.getFlight().getArrivalTimeUtc()
                                    .atZone(utcZone).withZoneSameInstant(shanghaiZone).toLocalDateTime() : null;
                            
                            analysis.put("departureInShanghai", depInShanghai);
                            analysis.put("arrivalInShanghai", arrInShanghai);
                            
                            
                            java.time.LocalDateTime depAsLocal = ticket.getFlight().getDepartureTimeUtc()
                                .atZone(utcZone).toLocalDateTime();
                            analysis.put("departureAsLocalDateTime", depAsLocal);
                            analysis.put("8HourEarlierIssue", "If UTC time " + ticket.getFlight().getDepartureTimeUtc() + 
                                " is shown as local time " + depAsLocal + ", it would appear 8 hours earlier than Shanghai time " + depInShanghai);
                            
                        } catch (Exception e) {
                            analysis.put("timezoneError", e.getMessage());
                        }
                    }
                    
                    ticketAnalysis.add(analysis);
                }
            }
            
            debugInfo.put("ticketAnalysis", ticketAnalysis);
            debugInfo.put("totalTickets", tickets.size());
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}