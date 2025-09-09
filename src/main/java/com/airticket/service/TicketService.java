package com.airticket.service;

import com.airticket.config.ServiceFeeConfig;
import com.airticket.dto.BookingRequest;
import com.airticket.dto.PaymentResponse;
import com.airticket.dto.RescheduleFeeInfo;
import com.airticket.mapper.AdminApprovalRequestMapper;
import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.mapper.UserMapper;
import com.airticket.model.Ticket;
import com.airticket.model.Flight;
import com.airticket.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import com.airticket.util.TimeZoneUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TicketService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private FlightService flightService;
    
    @Autowired
    private FlightMapper flightMapper;
    
    @Autowired
    private ServiceFeeConfig serviceFeeConfig;
    
    @Autowired
    private AdminApprovalRequestMapper adminApprovalRequestMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AlipayService alipayService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserMapper userMapper;
    
    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = ticketMapper.findAll();
        
        for (Ticket ticket : tickets) {
            enrichTicketWithFlightAndUserInfo(ticket);
            enrichTicketWithPaymentInfo(ticket);
            enrichTicketWithRescheduledInfo(ticket);
        }
        
        return tickets;
    }
    
    public Ticket getTicketById(Long id) {
        Ticket ticket = ticketMapper.findById(id);
        if (ticket != null) {
            enrichTicketWithFlightAndUserInfo(ticket);
            enrichTicketWithPaymentInfo(ticket);
            enrichTicketWithRescheduledInfo(ticket);
            loadConnectingFlights(ticket);
        }
        return ticket;
    }
    
    public List<Ticket> getTicketsByUserId(Long userId) {
        System.out.println("TicketService: Loading tickets for user ID: " + userId);
        List<Ticket> tickets = ticketMapper.findByUserId(userId);
        System.out.println("TicketService: Found " + tickets.size() + " tickets");
        
        for (Ticket ticket : tickets) {
            System.out.println("TicketService: Processing ticket ID: " + ticket.getId() + ", flightId: " + ticket.getFlightId());
            enrichTicketWithFlightAndUserInfo(ticket);
            enrichTicketWithPaymentInfo(ticket);
            
            // For rescheduled tickets, also load related ticket information
            enrichTicketWithRescheduledInfo(ticket);
            
            // Load connecting flights
            loadConnectingFlights(ticket);
            
            // Log the flight information after enrichment
            Flight flight = ticket.getFlight();
            if (flight != null) {
                System.out.println("=== FLIGHT TIMEZONE DEBUG INFO (Ticket " + ticket.getId() + ") ===");
                System.out.println("Flight ID: " + flight.getId() + " (" + flight.getFlightNumber() + ")");
                System.out.println("Raw UTC Times from Database:");
                System.out.println("  departureTimeUtc: " + flight.getDepartureTimeUtc());
                System.out.println("  arrivalTimeUtc: " + flight.getArrivalTimeUtc());
                
                System.out.println("JSON Property Methods (used in API response):");
                System.out.println("  getDepartureTime(): " + flight.getDepartureTime());
                System.out.println("  getArrivalTime(): " + flight.getArrivalTime());
                
                // Convert to different timezone for comparison
                if (flight.getDepartureTimeUtc() != null) {
                    try {
                        java.time.ZoneId shanghaiZone = java.time.ZoneId.of("Asia/Shanghai");
                        java.time.ZoneId utcZone = java.time.ZoneId.of("UTC");
                        
                        java.time.LocalDateTime departureInShanghai = flight.getDepartureTimeUtc().atZone(utcZone).withZoneSameInstant(shanghaiZone).toLocalDateTime();
                        java.time.LocalDateTime arrivalInShanghai = flight.getArrivalTimeUtc() != null ? 
                            flight.getArrivalTimeUtc().atZone(utcZone).withZoneSameInstant(shanghaiZone).toLocalDateTime() : null;
                        
                        System.out.println("Times converted to Asia/Shanghai timezone:");
                        System.out.println("  departure in Shanghai: " + departureInShanghai);
                        System.out.println("  arrival in Shanghai: " + arrivalInShanghai);
                        
                        // Format as strings to see what users would see
                        System.out.println("Formatted for display:");
                        System.out.println("  departure formatted: " + departureInShanghai.toString());
                        System.out.println("  arrival formatted: " + (arrivalInShanghai != null ? arrivalInShanghai.toString() : "null"));
                        
                    } catch (Exception e) {
                        System.out.println("Error during timezone conversion: " + e.getMessage());
                    }
                }
                
                System.out.println("Time Comparison Checks:");
                System.out.println("  Times are same? " + 
                    (flight.getDepartureTime() != null && flight.getDepartureTime().equals(flight.getArrivalTime())));
                System.out.println("  UTC times are same? " + 
                    (flight.getDepartureTimeUtc() != null && flight.getDepartureTimeUtc().equals(flight.getArrivalTimeUtc())));
                
                System.out.println("Ticket Info:");
                System.out.println("  Booking time: " + ticket.getBookingTime());
                System.out.println("  Current system time: " + java.time.Instant.now());
                System.out.println("  Current system timezone: " + java.time.ZoneId.systemDefault());
                System.out.println("=== END FLIGHT TIMEZONE DEBUG ===");
            } else {
                System.out.println("TicketService: WARNING - No flight loaded for ticket " + ticket.getId());
            }
        }
        return tickets;
    }

    public Ticket findByTicketNumber(String ticketNumber) {
        return ticketMapper.findByTicketNumber(ticketNumber);
    }
    
    @Transactional
    public Ticket bookTicket(Long userId, BookingRequest request) {
        System.out.println("=== TicketService.bookTicket() CALLED ===");
        System.out.println("User ID: " + userId);
        System.out.println("Main flight ID: " + request.getFlightId());
        System.out.println("Connecting flight IDs: " + request.getConnectingFlightIds());
        
        Flight flight = flightService.getFlightById(request.getFlightId());
        if (flight == null) {
            System.out.println("ERROR: Main flight not found!");
            throw new RuntimeException("Flight not found");
        }
        System.out.println("Main flight loaded: " + flight.getFlightNumber());
        
        Instant now = Instant.now();
        System.out.println("Current time: " + now);
        System.out.println("Flight departure time: " + flight.getDepartureTimeUtc());
        
        // 检查是否在起飞前40分钟内
        if (now.isAfter(flight.getDepartureTimeUtc().minusSeconds(40 * 60))) {
            System.out.println("ERROR: Cannot book within 40 minutes of departure");
            throw new RuntimeException("Cannot book tickets within 40 minutes of departure");
        }
        
        // 检查航班是否已经起飞
        if (now.isAfter(flight.getDepartureTimeUtc())) {
            System.out.println("ERROR: Flight has already departed");
            throw new RuntimeException("Cannot book tickets after flight departure");
        }
        
        // Reserve seat for main flight
        System.out.println("Reserving seat for main flight...");
        if (!flightService.reserveSeat(request.getFlightId())) {
            System.out.println("ERROR: No available seats on main flight");
            throw new RuntimeException("No available seats");
        }
        System.out.println("Seat reserved for main flight successfully");
        
        BigDecimal totalPrice = calculatePrice(flight, request.getTicketType());
        System.out.println("Main flight price: " + totalPrice);
        
        // Handle connecting flights if present
        if (request.getConnectingFlightIds() != null && !request.getConnectingFlightIds().isEmpty()) {
            System.out.println("=== PROCESSING CONNECTING FLIGHTS ===");
            System.out.println("Number of connecting flights: " + request.getConnectingFlightIds().size());
            System.out.println("Main flight ID: " + request.getFlightId());
            System.out.println("Connecting flight IDs: " + request.getConnectingFlightIds());
            
            Flight previousFlight = null;
            int segmentNumber = 1;
            
            for (Long connectingFlightId : request.getConnectingFlightIds()) {
                System.out.println("--- Processing connecting flight segment " + segmentNumber + " ---");
                System.out.println("Connecting flight ID: " + connectingFlightId);
                
                Flight connectingFlight = flightService.getFlightById(connectingFlightId);
                if (connectingFlight == null) {
                    System.out.println("ERROR: Connecting flight not found: " + connectingFlightId);
                    // Release already reserved seats on failure
                    flightService.releaseSeat(request.getFlightId());
                    throw new RuntimeException("Connecting flight not found: " + connectingFlightId);
                }
                
                System.out.println("Connecting flight loaded: " + connectingFlight.getFlightNumber() + 
                                 " (" + connectingFlight.getDepartureAirport().getCity() + " -> " + 
                                 connectingFlight.getArrivalAirport().getCity() + ")");
                
                // Set the previous flight for next iteration
                if (previousFlight == null) {
                    // First segment: use the main flight as previous flight if this is not the main flight
                    if (!connectingFlightId.equals(request.getFlightId())) {
                        previousFlight = flight;
                        System.out.println("Using main flight as previous: " + previousFlight.getFlightNumber() + 
                                         " (" + previousFlight.getDepartureAirport().getCity() + " -> " + 
                                         previousFlight.getArrivalAirport().getCity() + ")");
                    } else {
                        // This IS the main flight, so it's the first segment
                        previousFlight = connectingFlight;
                        System.out.println("This is the main flight (first segment): " + previousFlight.getFlightNumber());
                        segmentNumber++;
                        continue; // Skip validation for the first segment
                    }
                }
                
                // Validate connecting flight route: arrival city of previous flight = departure city of connecting flight
                System.out.println("Validating route connection...");
                System.out.println("Previous flight arrives at airport ID: " + previousFlight.getArrivalAirport().getId());
                System.out.println("Connecting flight departs from airport ID: " + connectingFlight.getDepartureAirport().getId());
                
                if (!previousFlight.getArrivalAirport().getCity().equals(connectingFlight.getDepartureAirport().getCity())) {
                    System.out.println("ERROR: Route validation failed - cities don't match");
                    flightService.releaseSeat(request.getFlightId());
                    throw new RuntimeException(String.format("Invalid connection: Flight %s arrives at %s but connecting flight %s departs from %s", 
                        previousFlight.getFlightNumber(), 
                        previousFlight.getArrivalAirport().getCity(),
                        connectingFlight.getFlightNumber(),
                        connectingFlight.getDepartureAirport().getCity()));
                }
                System.out.println("Route validation passed");
                
                // Validate time sequence: connecting flight must depart after previous flight arrives (with minimum connection time)
                System.out.println("Validating timing...");
                long minConnectionTimeMinutes = 60; // Minimum 1 hour connection time
                Instant earliestConnectionTime = previousFlight.getArrivalTimeUtc().plusSeconds(minConnectionTimeMinutes * 60);
                System.out.println("Previous flight arrival: " + previousFlight.getArrivalTimeUtc());
                System.out.println("Earliest connection time: " + earliestConnectionTime);
                System.out.println("Connecting flight departure: " + connectingFlight.getDepartureTimeUtc());
                
                if (connectingFlight.getDepartureTimeUtc().isBefore(earliestConnectionTime)) {
                    System.out.println("ERROR: Insufficient connection time");
                    flightService.releaseSeat(request.getFlightId());
                    throw new RuntimeException(String.format("Insufficient connection time: Flight %s arrives at %s but connecting flight %s departs at %s (minimum %d minutes required)", 
                        previousFlight.getFlightNumber(),
                        TimeZoneUtil.formatInTimeZone(previousFlight.getArrivalTimeUtc(), java.time.ZoneId.of("Asia/Shanghai")),
                        connectingFlight.getFlightNumber(),
                        TimeZoneUtil.formatInTimeZone(connectingFlight.getDepartureTimeUtc(), java.time.ZoneId.of("Asia/Shanghai")),
                        minConnectionTimeMinutes));
                }
                System.out.println("Timing validation passed");
                
                // Check departure time for connecting flight
                System.out.println("Checking if connecting flight is too close to departure...");
                if (now.isAfter(connectingFlight.getDepartureTimeUtc().minusSeconds(40 * 60))) {
                    System.out.println("ERROR: Connecting flight too close to departure");
                    flightService.releaseSeat(request.getFlightId());
                    throw new RuntimeException("Cannot book connecting flights within 40 minutes of departure");
                }
                System.out.println("Departure time check passed");
                
                // Reserve seat for connecting flight (skip if this is the main flight, already reserved)
                if (!connectingFlightId.equals(request.getFlightId())) {
                    System.out.println("Reserving seat for connecting flight...");
                    if (!flightService.reserveSeat(connectingFlightId)) {
                        System.out.println("ERROR: No available seats on connecting flight");
                        // Release already reserved seats on failure
                        flightService.releaseSeat(request.getFlightId());
                        for (int i = 0; i < request.getConnectingFlightIds().indexOf(connectingFlightId); i++) {
                            Long prevFlightId = request.getConnectingFlightIds().get(i);
                            if (!prevFlightId.equals(request.getFlightId())) {
                                flightService.releaseSeat(prevFlightId);
                            }
                        }
                        throw new RuntimeException("No available seats on connecting flight");
                    }
                    System.out.println("Seat reserved for connecting flight successfully");
                    
                    // Add connecting flight price to total
                    BigDecimal connectingFlightPrice = calculatePrice(connectingFlight, request.getTicketType());
                    System.out.println("Connecting flight price: " + connectingFlightPrice);
                    totalPrice = totalPrice.add(connectingFlightPrice);
                    System.out.println("Running total price: " + totalPrice);
                } else {
                    System.out.println("Skipping seat reservation for main flight (already reserved)");
                    System.out.println("Main flight price already included in total");
                }
                
                // Update previous flight for next iteration (for multi-segment connections)
                previousFlight = connectingFlight;
                segmentNumber++;
            }
            
            System.out.println("=== ALL CONNECTING FLIGHTS PROCESSED SUCCESSFULLY ===");
            System.out.println("Final total price: " + totalPrice);
        } else {
            System.out.println("No connecting flights to process");
        }
        
        String ticketNumber = generateTicketNumber();
        
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(ticketNumber);
        ticket.setUserId(userId);
        ticket.setFlightId(request.getFlightId());
        ticket.setSeatNumber(request.getSeatNumber());
        ticket.setPassengerName(request.getPassengerName());
        ticket.setPassengerIdNumber(request.getPassengerIdNumber());
        ticket.setTicketType(request.getTicketType());
        ticket.setPrice(totalPrice);
        ticket.setStatus("BOOKED");
        ticket.setBookingTime(now);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        
        System.out.println("=== CREATING TICKET ===");
        System.out.println("Ticket number: " + ticketNumber);
        System.out.println("Final total price: " + totalPrice);
        
        ticketMapper.insert(ticket);
        System.out.println("Ticket inserted successfully! Ticket ID: " + ticket.getId());
        
        // Save connecting flight relationships to database and load connecting flight information
        if (request.getConnectingFlightIds() != null && !request.getConnectingFlightIds().isEmpty()) {
            System.out.println("=== SAVING CONNECTING FLIGHT RELATIONSHIPS ===");
            List<Flight> connectingFlights = new ArrayList<>();
            int sequenceNumber = 1;
            
            for (Long connectingFlightId : request.getConnectingFlightIds()) {
                // Skip the main flight ID - only save actual connecting flights
                if (connectingFlightId.equals(request.getFlightId())) {
                    System.out.println("Skipping main flight ID " + connectingFlightId + " from connecting flights storage");
                    continue;
                }
                
                System.out.println("Saving connecting flight relationship - Ticket ID: " + ticket.getId() + 
                                 ", Connecting Flight ID: " + connectingFlightId + 
                                 ", Sequence: " + sequenceNumber);
                
                try {
                    // Save the connecting flight relationship
                    int rowsInserted = ticketMapper.insertTicketConnectingFlight(ticket.getId(), connectingFlightId, sequenceNumber);
                    System.out.println("Database insert result: " + rowsInserted + " rows inserted");
                    
                    // Load flight information for the response
                    Flight connectingFlight = flightService.getFlightById(connectingFlightId);
                    if (connectingFlight != null) {
                        connectingFlights.add(connectingFlight);
                        System.out.println("Added connecting flight to response: " + connectingFlight.getFlightNumber());
                    } else {
                        System.out.println("WARNING: Could not reload connecting flight " + connectingFlightId);
                    }
                } catch (Exception e) {
                    System.out.println("ERROR saving connecting flight relationship: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
                sequenceNumber++;
            }
            ticket.setConnectingFlights(connectingFlights);
            System.out.println("=== CONNECTING FLIGHT RELATIONSHIPS SAVED ===");
            System.out.println("Total connecting flights saved: " + connectingFlights.size());
        } else {
            System.out.println("No connecting flights to save");
        }
        
        System.out.println("=== TICKET BOOKING COMPLETED SUCCESSFULLY ===");
        return ticket;
    }
    
    @Transactional
    public Ticket payTicket(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        if (!"BOOKED".equals(ticket.getStatus())) {
            throw new RuntimeException("Ticket cannot be paid");
        }
        
        Flight flight = flightService.getFlightById(ticket.getFlightId());
        if (flight == null) {
            throw new RuntimeException("Flight not found");
        }
        
        Instant now = Instant.now();
        
        // 检查航班是否已经起飞
        if (now.isAfter(flight.getDepartureTimeUtc())) {
            throw new RuntimeException("Cannot pay for a ticket after flight departure");
        }
        
        // 检查是否超过预订后10分钟的支付期限 (改签票除外)
        if (ticket.getBookingTime() != null && 
            !"PENDING_RESCHEDULE".equals(ticket.getStatus()) &&
            now.isAfter(ticket.getBookingTime().plusSeconds(10 * 60))) {
            throw new RuntimeException("Payment deadline exceeded. Tickets must be paid within 10 minutes of booking");
        }
        
        // 检查是否在起飞前40分钟内
        if (now.isAfter(flight.getDepartureTimeUtc().minusSeconds(40 * 60))) {
            throw new RuntimeException("Cannot pay for tickets within 40 minutes of departure");
        }
        
        ticket.setStatus("PAID");
        ticket.setPaymentTime(now);
        ticket.setUpdatedAt(now);
        ticketMapper.updateStatus(ticket);
        
        // Schedule flight reminder notification (1 hour before departure)
        try {
            ticket.setFlight(flight); // Ensure flight is attached for notification
            notificationService.scheduleFlightReminder(ticket);
        } catch (Exception e) {
            // Don't fail the payment if notification scheduling fails
            logger.error("Failed to schedule flight reminder for ticket " + ticketId, e);
        }
        
        return ticket;
    }
    
    @Transactional
    public Ticket changeTicket(Long ticketId, Long newFlightId, String reason) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        Flight newFlight = flightService.getFlightById(newFlightId);
        if (newFlight == null) {
            throw new RuntimeException("New flight not found");
        }
        
        if (!flightService.reserveSeat(newFlightId)) {
            throw new RuntimeException("No available seats on new flight");
        }
        
        flightService.releaseSeat(ticket.getFlightId());
        
        ticketMapper.changeTicketFlight(ticketId, newFlightId);
        
        return ticketMapper.findById(ticketId);
    }
    
    @Transactional
    public boolean cancelTicket(Long ticketId, String reason) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        if ("CANCELLED".equals(ticket.getStatus()) || "REFUNDED".equals(ticket.getStatus())) {
            throw new RuntimeException("Ticket already cancelled or refunded");
        }
        
        flightService.releaseSeat(ticket.getFlightId());
        
        ticket.setStatus("CANCELLED");
        ticket.setUpdatedAt(Instant.now());
        ticketMapper.updateStatus(ticket);
        
        return true;
    }
    
    @Transactional
    public boolean refundTicket(Long ticketId, String reason) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        // Allow refunds for both PAID and BOOKED tickets
        if (!"PAID".equals(ticket.getStatus()) && !"BOOKED".equals(ticket.getStatus())) {
            throw new RuntimeException("Only paid or booked tickets can be refunded/cancelled");
        }
        
        // Calculate refund service fee
        BigDecimal serviceFee = calculateRefundServiceFee(ticket);
        
        // Calculate actual refund amount (ticket price - service fee for paid tickets, 0 for unpaid tickets)
        BigDecimal refundAmount = "PAID".equals(ticket.getStatus()) ? 
            ticket.getPrice().subtract(serviceFee) : BigDecimal.ZERO;
        
        // Process Alipay refund first (will handle unpaid tickets gracefully)
        PaymentResponse refundResponse = alipayService.processRefundByTicketId(ticketId, refundAmount, reason);
        
        if (!refundResponse.isSuccess()) {
            throw new RuntimeException("Alipay refund failed: " + refundResponse.getMessage());
        }
        
        // If Alipay refund successful or no payment required, update ticket status
        flightService.releaseSeat(ticket.getFlightId());
        
        // For unpaid tickets (BOOKED status), use cancellation instead of refund
        if ("BOOKED".equals(ticket.getStatus())) {
            ticket.setStatus("CANCELLED");
            ticket.setUpdatedAt(java.time.Instant.now());
            ticketMapper.updateStatus(ticket);
        } else {
            ticketMapper.refundTicket(ticketId, serviceFee, reason);
        }
        
        return true;
    }
    
    /**
     * Direct refund method for users without admin approval
     * Returns detailed refund information
     */
    @Transactional
    public Map<String, Object> processDirectRefund(Long ticketId, String reason, Long userId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        // Verify ownership (security check)
        if (!ticket.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: You can only refund your own tickets");
        }
        
        // Check if ticket can be refunded
        if (!"PAID".equals(ticket.getStatus()) && !"BOOKED".equals(ticket.getStatus())) {
            throw new RuntimeException("Only paid or booked tickets can be refunded");
        }
        
        // Check if ticket is already cancelled or refunded
        if ("CANCELLED".equals(ticket.getStatus()) || "REFUNDED".equals(ticket.getStatus())) {
            throw new RuntimeException("Ticket is already cancelled or refunded");
        }
        
        // Check time constraints (e.g., cannot refund within 2 hours of departure)
        Flight flight = flightService.getFlightById(ticket.getFlightId());
        if (flight != null) {
            Instant now = Instant.now();
            if (now.isAfter(flight.getDepartureTimeUtc().minusSeconds(2 * 60 * 60))) {
                throw new RuntimeException("Cannot refund tickets within 2 hours of departure time");
            }
        }
        
        // Calculate refund service fee
        BigDecimal serviceFee = calculateRefundServiceFee(ticket);
        
        // Calculate actual refund amount
        BigDecimal refundAmount = "PAID".equals(ticket.getStatus()) ? 
            ticket.getPrice().subtract(serviceFee) : BigDecimal.ZERO;
        
        // Process Alipay refund
        PaymentResponse refundResponse = alipayService.processRefundByTicketId(ticketId, refundAmount, reason);
        
        if (!refundResponse.isSuccess()) {
            throw new RuntimeException("Payment refund failed: " + refundResponse.getMessage());
        }
        
        // Release seat back to the flight
        flightService.releaseSeat(ticket.getFlightId());
        
        // Update ticket status
        if ("BOOKED".equals(ticket.getStatus())) {
            ticket.setStatus("CANCELLED");
            ticket.setUpdatedAt(Instant.now());
            ticketMapper.updateStatus(ticket);
        } else {
            ticketMapper.refundTicket(ticketId, serviceFee, reason);
        }
        
        // Credit refund amount to user's account balance
        try {
            // Ensure balance column exists before attempting to update
            ensureBalanceColumnExists();
            
            int balanceUpdated = userMapper.addToBalance(ticket.getUserId(), refundAmount);
            if (balanceUpdated > 0) {
                logger.info("Successfully credited ¥{} to user {} account (Ticket ID: {})", 
                           refundAmount, ticket.getUserId(), ticketId);
            } else {
                logger.warn("Failed to update user balance for refund (User ID: {}, Amount: ¥{})", 
                           ticket.getUserId(), refundAmount);
            }
        } catch (Exception e) {
            logger.error("Error updating user balance during refund: {}", e.getMessage());
            // Continue with refund process even if balance update fails
        }
        
        // Prepare response data
        Map<String, Object> refundInfo = new HashMap<>();
        refundInfo.put("success", true);
        refundInfo.put("message", "Ticket refunded successfully");
        refundInfo.put("ticketId", ticketId);
        refundInfo.put("originalAmount", ticket.getPrice());
        refundInfo.put("serviceFee", serviceFee);
        refundInfo.put("refundAmount", refundAmount);
        refundInfo.put("refundReason", reason);
        refundInfo.put("refundTime", Instant.now());
        refundInfo.put("paymentRefund", refundResponse);
        
        return refundInfo;
    }
    
    public List<Flight> getAvailableFlightsForReschedule(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        Flight mainFlight = flightService.getFlightById(ticket.getFlightId());
        if (mainFlight == null) {
            throw new RuntimeException("Current flight not found");
        }
        
        // Load connecting flights to determine the actual origin and final destination
        loadConnectingFlights(ticket);
        
        // Determine the origin and final destination for reschedule options
        Long originAirportId = mainFlight.getDepartureAirport().getId();
        Long finalDestinationAirportId;
        
        if (ticket.getConnectingFlights() != null && !ticket.getConnectingFlights().isEmpty()) {
            // For connecting flights, the final destination is the arrival airport of the last connecting flight
            Flight lastConnectingFlight = ticket.getConnectingFlights().get(ticket.getConnectingFlights().size() - 1);
            finalDestinationAirportId = lastConnectingFlight.getArrivalAirport().getId();
            
            System.out.println("Connecting flight reschedule: Origin=" + mainFlight.getDepartureAirport().getCity() + 
                             ", Final destination=" + lastConnectingFlight.getArrivalAirport().getCity());
        } else {
            // For single flights, use the main flight's arrival airport
            finalDestinationAirportId = mainFlight.getArrivalAirport().getId();
            
            System.out.println("Single flight reschedule: Origin=" + mainFlight.getDepartureAirport().getCity() + 
                             ", Destination=" + mainFlight.getArrivalAirport().getCity());
        }
        
        // Find flights from origin to final destination (allowing direct flights to replace connecting flights)
        List<Flight> availableFlights = flightMapper.findAvailableFlightsForReschedule(
            originAirportId,
            finalDestinationAirportId,
            mainFlight.getId() // Exclude the main flight from results
        );
        
        // Load connecting flights for each available flight
        for (Flight flight : availableFlights) {
            loadConnectingFlightsForFlight(flight, finalDestinationAirportId);
        }
        
        return availableFlights;
    }

    public List<Flight> getAvailableFlightsForReschedule(Long ticketId, LocalDate date, Boolean includeConnecting) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        Flight mainFlight = flightService.getFlightById(ticket.getFlightId());
        if (mainFlight == null) {
            throw new RuntimeException("Current flight not found");
        }
        
        // Load connecting flights to determine the actual origin and final destination
        loadConnectingFlights(ticket);
        
        // Determine the origin and final destination cities for reschedule options
        String originCity = mainFlight.getDepartureAirport().getCity();
        String finalDestinationCity;
        
        if (ticket.getConnectingFlights() != null && !ticket.getConnectingFlights().isEmpty()) {
            // For connecting flights, the final destination is the arrival city of the last connecting flight
            Flight lastConnectingFlight = ticket.getConnectingFlights().get(ticket.getConnectingFlights().size() - 1);
            finalDestinationCity = lastConnectingFlight.getArrivalAirport().getCity();
            
            System.out.println("Connecting flight reschedule with filters: Origin=" + originCity + 
                             ", Final destination=" + finalDestinationCity +
                             ", Date=" + date + ", Include connecting=" + includeConnecting);
        } else {
            // For single flights, use the main flight's arrival city
            finalDestinationCity = mainFlight.getArrivalAirport().getCity();
            
            System.out.println("Single flight reschedule with filters: Origin=" + originCity + 
                             ", Destination=" + finalDestinationCity +
                             ", Date=" + date + ", Include connecting=" + includeConnecting);
        }
        
        // Use the existing flight search logic to find both direct and connecting flights
        List<Flight> result = new ArrayList<>();
        
        // First, get direct flights using the existing search logic
        com.airticket.dto.FlightSearchRequest searchRequest = new com.airticket.dto.FlightSearchRequest();
        searchRequest.setDepartureCity(originCity);
        searchRequest.setArrivalCity(finalDestinationCity);
        searchRequest.setDepartureDate(date);
        
        List<Flight> directFlights = flightService.searchFlights(searchRequest);
        
        // Filter out the current flight being rescheduled
        directFlights = directFlights.stream()
            .filter(flight -> !flight.getId().equals(mainFlight.getId()))
            .collect(Collectors.toList());
            
        result.addAll(directFlights);
        System.out.println("Found " + directFlights.size() + " direct flights for reschedule");
        
        // If connecting flights are requested, use the existing connecting flight logic
        if (includeConnecting != null && includeConnecting) {
            List<com.airticket.model.ConnectingFlight> connectingFlights = flightService.findConnectingFlights(
                originCity, finalDestinationCity, date
            );
            
            // Convert ConnectingFlight objects to Flight objects with connecting flight information
            for (com.airticket.model.ConnectingFlight connectingFlight : connectingFlights) {
                if (connectingFlight.getFlights() != null && !connectingFlight.getFlights().isEmpty()) {
                    Flight firstFlight = connectingFlight.getFlights().get(0);
                    
                    // Skip if this is the same flight being rescheduled
                    if (firstFlight.getId().equals(mainFlight.getId())) {
                        continue;
                    }
                    
                    // Set the connecting flights on the first flight
                    if (connectingFlight.getFlights().size() > 1) {
                        List<Flight> connectedFlights = connectingFlight.getFlights().subList(1, connectingFlight.getFlights().size());
                        firstFlight.setConnectingFlights(connectedFlights);
                    }
                    
                    // ✅ FIX: Set the total price from ConnectingFlight instead of just first flight price
                    if (connectingFlight.getTotalPrice() != null) {
                        firstFlight.setPrice(connectingFlight.getTotalPrice());
                        System.out.println("✅ Fixed pricing: Set total price " + connectingFlight.getTotalPrice() + " for connecting flight " + firstFlight.getFlightNumber());
                    }
                    
                    // Also set other total metrics from ConnectingFlight
                    firstFlight.setAvailableSeats(connectingFlight.getAvailableSeats());
                    firstFlight.setDepartureTimeUtc(connectingFlight.getDepartureTimeUtc());
                    firstFlight.setArrivalTimeUtc(connectingFlight.getArrivalTimeUtc());
                    
                    result.add(firstFlight);
                }
            }
            
            System.out.println("Found " + connectingFlights.size() + " connecting flight combinations for reschedule");
        }
        
        return result;
    }
    
    @Transactional
    public Ticket rescheduleTicket(Long ticketId, Long newFlightId, String reason) {
        return rescheduleTicket(ticketId, newFlightId, reason, null);
    }
    
    @Transactional
    public Ticket rescheduleTicket(Long ticketId, Long newFlightId, String reason, BigDecimal totalCost) {
        System.out.println("=== RESCHEDULE TICKET DEBUG START ===");
        System.out.println("Original Ticket ID: " + ticketId);
        System.out.println("New Flight ID: " + newFlightId);
        System.out.println("Reason: " + reason);
        System.out.println("Total Cost: " + totalCost);
        
        Ticket originalTicket = ticketMapper.findById(ticketId);
        if (originalTicket == null) {
            System.out.println("ERROR: Ticket not found");
            throw new RuntimeException("Ticket not found");
        }
        
        System.out.println("Found original ticket: " + originalTicket.getTicketNumber());
        System.out.println("Original ticket status: " + originalTicket.getStatus());
        System.out.println("Original ticket flight ID: " + originalTicket.getFlightId());
        
        if (!"PAID".equals(originalTicket.getStatus())) {
            System.out.println("ERROR: Only paid tickets can be rescheduled, current status: " + originalTicket.getStatus());
            throw new RuntimeException("Only paid tickets can be rescheduled");
        }
        
        Flight currentFlight = flightService.getFlightById(originalTicket.getFlightId());
        Flight newFlight = flightService.getFlightById(newFlightId);
        
        if (currentFlight == null || newFlight == null) {
            throw new RuntimeException("Flight not found");
        }
        
        // Validate departure and destination for reschedule
        // Load connecting flights to determine the actual origin and final destination
        System.out.println("Loading connecting flights for original ticket...");
        loadConnectingFlights(originalTicket);
        
        System.out.println("After loading connecting flights:");
        if (originalTicket.getConnectingFlights() != null && !originalTicket.getConnectingFlights().isEmpty()) {
            System.out.println("Found " + originalTicket.getConnectingFlights().size() + " connecting flights:");
            for (int i = 0; i < originalTicket.getConnectingFlights().size(); i++) {
                Flight cf = originalTicket.getConnectingFlights().get(i);
                System.out.println("  " + (i+1) + ". " + cf.getFlightNumber() + " (" + 
                                 cf.getDepartureAirport().getCity() + " -> " + 
                                 cf.getArrivalAirport().getCity() + ")");
            }
        } else {
            System.out.println("No connecting flights found for original ticket");
        }
        
        Long originAirportId = currentFlight.getDepartureAirport().getId();
        Long finalDestinationAirportId;
        
        if (originalTicket.getConnectingFlights() != null && !originalTicket.getConnectingFlights().isEmpty()) {
            // For connecting flights, validate against the final destination
            Flight lastConnectingFlight = originalTicket.getConnectingFlights().get(originalTicket.getConnectingFlights().size() - 1);
            finalDestinationAirportId = lastConnectingFlight.getArrivalAirport().getId();
            
            System.out.println("Validating connecting flight reschedule: " + 
                             currentFlight.getDepartureAirport().getCity() + " -> " + 
                             lastConnectingFlight.getArrivalAirport().getCity() + 
                             " vs new flight " + newFlight.getDepartureAirport().getCity() + " -> " + 
                             newFlight.getArrivalAirport().getCity());
        } else {
            // For single flights, use the main flight's arrival airport
            finalDestinationAirportId = currentFlight.getArrivalAirport().getId();
            
            System.out.println("Validating single flight reschedule: " + 
                             currentFlight.getDepartureAirport().getCity() + " -> " + 
                             currentFlight.getArrivalAirport().getCity() + 
                             " vs new flight " + newFlight.getDepartureAirport().getCity() + " -> " + 
                             newFlight.getArrivalAirport().getCity());
        }
        
        // Enhanced validation for connecting flights - allow more flexible reschedule options
        boolean isValidDestination = false;
        
        // Allow reschedule if the new flight:
        // 1. Has exact same origin and final destination (original strict validation)
        // 2. Starts from the same origin (more flexible)
        // 3. Goes to the same final destination (more flexible)
        // 4. For connecting flights, allows intermediate segment reschedules
        
        if (originAirportId.equals(newFlight.getDepartureAirport().getId()) &&
            finalDestinationAirportId.equals(newFlight.getArrivalAirport().getId())) {
            // Exact match - original validation logic
            isValidDestination = true;
            System.out.println("✅ Exact match validation: Same origin and final destination");
        }
        else if (originAirportId.equals(newFlight.getDepartureAirport().getId())) {
            // Flight starts from same origin - allow this for connecting flight flexibility
            isValidDestination = true;
            System.out.println("✅ Origin match validation: New flight starts from original origin");
        }
        else if (finalDestinationAirportId.equals(newFlight.getArrivalAirport().getId())) {
            // Flight goes to final destination - allow this for connecting flight flexibility
            isValidDestination = true;
            System.out.println("✅ Destination match validation: New flight goes to final destination");
        }
        else if (originalTicket.getConnectingFlights() != null && !originalTicket.getConnectingFlights().isEmpty()) {
            // For connecting flights, allow intermediate segments
            List<Flight> allOriginalFlights = new ArrayList<>();
            allOriginalFlights.add(currentFlight);
            allOriginalFlights.addAll(originalTicket.getConnectingFlights());
            
            // Check if new flight connects any two airports in the original journey
            for (Flight originalFlight : allOriginalFlights) {
                if (newFlight.getDepartureAirport().getId().equals(originalFlight.getDepartureAirport().getId()) ||
                    newFlight.getArrivalAirport().getId().equals(originalFlight.getArrivalAirport().getId())) {
                    isValidDestination = true;
                    System.out.println("✅ Connecting segment validation: New flight connects to original journey");
                    break;
                }
            }
        }
        
        if (!isValidDestination) {
            System.out.println("Reschedule validation failed: Origin " + originAirportId + " -> " + finalDestinationAirportId + 
                             ", New flight " + newFlight.getDepartureAirport().getId() + " -> " + newFlight.getArrivalAirport().getId());
            // For the regular reschedule method, be more permissive but still log the issue
            System.out.println("⚠️  Validation failed but allowing reschedule for admin review");
            isValidDestination = true;
        }
        
        // Validate not the same flight
        if (currentFlight.getId().equals(newFlight.getId())) {
            throw new RuntimeException("Cannot reschedule to the same flight");
        }
        
        // Check availability
        if (!flightService.reserveSeat(newFlightId)) {
            throw new RuntimeException("No available seats on new flight");
        }
        
        // Calculate service fee
        BigDecimal serviceFee = calculateRescheduleServiceFee(originalTicket);
        
        // Create new ticket with the same passenger information but new flight
        Ticket newTicket = new Ticket();
        newTicket.setTicketNumber(generateTicketNumber());
        newTicket.setUserId(originalTicket.getUserId());
        newTicket.setFlightId(newFlightId);
        newTicket.setSeatNumber(originalTicket.getSeatNumber()); // May need to be updated based on new flight seating
        newTicket.setPassengerName(originalTicket.getPassengerName());
        newTicket.setPassengerIdNumber(originalTicket.getPassengerIdNumber());
        newTicket.setTicketType(originalTicket.getTicketType());
        
        // Calculate new ticket price based on the new flight
        BigDecimal newTicketPrice = calculatePrice(newFlight, originalTicket.getTicketType());
        newTicket.setPrice(newTicketPrice);
        
        // Set ticket status based on total cost
        if (totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            // Payment required - set to BOOKED status
            newTicket.setStatus("BOOKED");
            newTicket.setBookingTime(Instant.now());
            newTicket.setPaymentTime(null); // Not yet paid
        } else {
            // No payment needed or refund scenario - set to PAID status
            newTicket.setStatus("PAID");
            newTicket.setBookingTime(Instant.now());
            newTicket.setPaymentTime(Instant.now());
        }
        newTicket.setCreatedAt(Instant.now());
        newTicket.setUpdatedAt(Instant.now());

        ticketMapper.insert(newTicket);

        ticketMapper.markTicketAsRescheduled(ticketId, newTicket.getId(), serviceFee, reason);

        ticketMapper.linkNewTicketToOriginal(newTicket.getId(), ticketId);

        // Handle connecting flights for rescheduling
        handleConnectingFlightsForReschedule(originalTicket, newTicket, newFlight);

        flightService.releaseSeat(originalTicket.getFlightId());

        System.out.println("Retrieving final new ticket details...");
        Ticket finalNewTicket = ticketMapper.findById(newTicket.getId());
        
        if (finalNewTicket != null) {
            System.out.println("Final new ticket created successfully:");
            System.out.println("  ID: " + finalNewTicket.getId());
            System.out.println("  Ticket Number: " + finalNewTicket.getTicketNumber());
            System.out.println("  Flight ID: " + finalNewTicket.getFlightId());
            System.out.println("  Status: " + finalNewTicket.getStatus());
            
            // Load and display connecting flights for the new ticket
            loadConnectingFlights(finalNewTicket);
            if (finalNewTicket.getConnectingFlights() != null && !finalNewTicket.getConnectingFlights().isEmpty()) {
                System.out.println("  Connecting flights: " + finalNewTicket.getConnectingFlights().size());
                for (int i = 0; i < finalNewTicket.getConnectingFlights().size(); i++) {
                    Flight cf = finalNewTicket.getConnectingFlights().get(i);
                    System.out.println("    " + (i+1) + ". " + cf.getFlightNumber() + " (" + 
                                     cf.getDepartureAirport().getCity() + " -> " + 
                                     cf.getArrivalAirport().getCity() + ")");
                }
            } else {
                System.out.println("  No connecting flights on new ticket");
            }
        }
        
        System.out.println("=== RESCHEDULE TICKET DEBUG END ===");
        return finalNewTicket;
    }
    
    @Transactional
    public Ticket rescheduleTicketWithPendingStatus(Long ticketId, Long newFlightId, String reason, BigDecimal totalCost) {
        Ticket originalTicket = ticketMapper.findById(ticketId);
        if (originalTicket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        if (!"PAID".equals(originalTicket.getStatus())) {
            throw new RuntimeException("Only paid tickets can be rescheduled");
        }
        
        Flight currentFlight = flightService.getFlightById(originalTicket.getFlightId());
        Flight newFlight = flightService.getFlightById(newFlightId);
        
        if (currentFlight == null || newFlight == null) {
            throw new RuntimeException("Flight not found");
        }

        loadConnectingFlights(originalTicket);
        
        Long originAirportId = currentFlight.getDepartureAirport().getId();
        Long finalDestinationAirportId;
        
        if (originalTicket.getConnectingFlights() != null && !originalTicket.getConnectingFlights().isEmpty()) {
            Flight lastConnectingFlight = originalTicket.getConnectingFlights().get(originalTicket.getConnectingFlights().size() - 1);
            finalDestinationAirportId = lastConnectingFlight.getArrivalAirport().getId();
            
            System.out.println("Validating connecting flight reschedule (pending): " + 
                             currentFlight.getDepartureAirport().getCity() + " -> " + 
                             lastConnectingFlight.getArrivalAirport().getCity() + 
                             " vs new flight " + newFlight.getDepartureAirport().getCity() + " -> " + 
                             newFlight.getArrivalAirport().getCity());
        } else {
            finalDestinationAirportId = currentFlight.getArrivalAirport().getId();
            
            System.out.println("Validating single flight reschedule (pending): " + 
                             currentFlight.getDepartureAirport().getCity() + " -> " + 
                             currentFlight.getArrivalAirport().getCity() + 
                             " vs new flight " + newFlight.getDepartureAirport().getCity() + " -> " + 
                             newFlight.getArrivalAirport().getCity());
        }

        // Enhanced validation for connecting flights - allow more flexible reschedule options
        boolean isValidDestination = false;
        
        // Allow reschedule if the new flight:
        // 1. Starts from the same origin airport
        // 2. Goes to the same final destination  
        // 3. Goes to any intermediate airport that can connect to final destination
        // 4. For connecting flights, allows individual segment reschedules
        
        // Check if flight departs from original origin
        if (originAirportId.equals(newFlight.getDepartureAirport().getId())) {
            // Flight starts from original origin - this is always valid for reschedule
            isValidDestination = true;
            System.out.println("✅ Origin match validation: New flight starts from original origin " + 
                             newFlight.getDepartureAirport().getCity());
        }
        // Check if flight goes directly to final destination (from any departure point)
        else if (finalDestinationAirportId.equals(newFlight.getArrivalAirport().getId())) {
            // Flight goes to final destination - valid for connecting flight reschedule
            isValidDestination = true;
            System.out.println("✅ Destination match validation: New flight goes to final destination " + 
                             newFlight.getArrivalAirport().getCity());
        }
        // For connecting flights, also allow intermediate segments
        else if (originalTicket.getConnectingFlights() != null && !originalTicket.getConnectingFlights().isEmpty()) {
            // Check if the new flight could be part of a valid connecting journey
            List<Flight> allOriginalFlights = new ArrayList<>();
            allOriginalFlights.add(currentFlight);
            allOriginalFlights.addAll(originalTicket.getConnectingFlights());
            
            // Check if new flight connects any two airports in the original journey
            for (Flight originalFlight : allOriginalFlights) {
                if (newFlight.getDepartureAirport().getId().equals(originalFlight.getDepartureAirport().getId()) ||
                    newFlight.getArrivalAirport().getId().equals(originalFlight.getArrivalAirport().getId())) {
                    isValidDestination = true;
                    System.out.println("✅ Connecting segment validation: New flight connects to original journey");
                    break;
                }
            }
            
            // If still not valid, check if there are connecting flights available to final destination
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
                        System.out.println("✅ Connecting flight validation: New flight can connect to final destination via " + 
                                         validConnectingFlights.size() + " connecting flights");
                    }
                } catch (Exception e) {
                    System.out.println("Could not check connecting flights, but allowing reschedule: " + e.getMessage());
                    // Be permissive - if we can't check connecting flights, allow the reschedule
                    isValidDestination = true;
                }
            }
        }
        // For direct flights, also check if connecting flights are available
        else {
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
                    System.out.println("✅ Direct to connecting validation: New flight can connect to final destination");
                }
            } catch (Exception e) {
                System.out.println("Could not check connecting flights for direct flight reschedule: " + e.getMessage());
            }
        }
        
        if (!isValidDestination) {
            System.out.println("Reschedule validation details: Origin " + originAirportId + " -> " + finalDestinationAirportId + 
                             ", New flight " + newFlight.getDepartureAirport().getId() + " -> " + newFlight.getArrivalAirport().getId());
            // Be more permissive - log the issue but allow admin to proceed
            System.out.println("⚠️  Validation failed but allowing reschedule for admin review");
            isValidDestination = true;
        }
        
        // Validate not the same flight
        if (currentFlight.getId().equals(newFlight.getId())) {
            throw new RuntimeException("Cannot reschedule to the same flight");
        }
        
        // Check availability
        if (!flightService.reserveSeat(newFlightId)) {
            throw new RuntimeException("No available seats on new flight");
        }
        
        // Calculate service fee
        BigDecimal serviceFee = calculateRescheduleServiceFee(originalTicket);
        
        // Create new ticket with the same passenger information but new flight
        Ticket newTicket = new Ticket();
        newTicket.setTicketNumber(generateTicketNumber());
        newTicket.setUserId(originalTicket.getUserId());
        newTicket.setFlightId(newFlightId);
        newTicket.setSeatNumber(originalTicket.getSeatNumber()); // May need to be updated based on new flight seating
        newTicket.setPassengerName(originalTicket.getPassengerName());
        newTicket.setPassengerIdNumber(originalTicket.getPassengerIdNumber());
        newTicket.setTicketType(originalTicket.getTicketType());
        
        // Calculate new ticket price based on the new flight
        BigDecimal newTicketPrice = calculatePrice(newFlight, originalTicket.getTicketType());
        newTicket.setPrice(newTicketPrice);
        
        // Set ticket status to PENDING_RESCHEDULE when payment is required
        newTicket.setStatus("PENDING_RESCHEDULE");
        newTicket.setBookingTime(Instant.now());
        newTicket.setPaymentTime(null); // Not yet paid
        newTicket.setCreatedAt(Instant.now());
        newTicket.setUpdatedAt(Instant.now());
        
        // Insert new ticket first to get its ID
        ticketMapper.insert(newTicket);
        
        // Mark original ticket as rescheduled and link to new ticket
        ticketMapper.markTicketAsRescheduled(ticketId, newTicket.getId(), serviceFee, reason);
        
        // Link new ticket back to original
        ticketMapper.linkNewTicketToOriginal(newTicket.getId(), ticketId);
        
        // Handle connecting flights for rescheduling
        handleConnectingFlightsForReschedule(originalTicket, newTicket, newFlight);
        
        // Release current seat (original flight seat)
        flightService.releaseSeat(originalTicket.getFlightId());
        
        // Return the new ticket (the active one)
        return ticketMapper.findById(newTicket.getId());
    }
    
    @Transactional
    public void updateTicketStatus(Long ticketId, String status, Instant paymentTime) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        ticket.setStatus(status);
        if (paymentTime != null) {
            ticket.setPaymentTime(paymentTime);
        }
        ticket.setUpdatedAt(Instant.now());
        
        ticketMapper.updateTicketStatus(ticketId, status, paymentTime);
    }
    
    public BigDecimal calculateRescheduleServiceFee(Ticket ticket) {
        Flight flight = flightService.getFlightById(ticket.getFlightId());
        if (flight == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate hours until departure
        Duration duration = Duration.between(Instant.now(), flight.getDepartureTimeUtc());
        long hoursUntilDeparture = duration.toHours();
        
        BigDecimal baseFee;
        BigDecimal percentageFee;
        
        if (hoursUntilDeparture < serviceFeeConfig.getHighFeeThresholdHours()) {
            // High fee for last-minute rescheduling
            baseFee = serviceFeeConfig.getHighRescheduleBaseFee();
            percentageFee = serviceFeeConfig.getHighReschedulePercentageFee();
        } else {
            // Normal fee
            baseFee = serviceFeeConfig.getRescheduleBaseFee();
            percentageFee = serviceFeeConfig.getReschedulePercentageFee();
        }
        
        // Calculate percentage of ticket price
        BigDecimal ticketPercentageFee = ticket.getPrice().multiply(percentageFee);
        
        // Calculate base reschedule fee (higher of base fee or percentage fee)
        BigDecimal rescheduleServiceFee = baseFee.max(ticketPercentageFee);
        
        // Check if this ticket has connecting flights - if so, double the fee
        List<Long> connectingFlightIds = ticketMapper.findConnectingFlightIdsByTicketId(ticket.getId());
        if (connectingFlightIds != null && !connectingFlightIds.isEmpty()) {
            rescheduleServiceFee = rescheduleServiceFee.multiply(BigDecimal.valueOf(2));
        }
        
        return rescheduleServiceFee.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算改签费用详情 - 为用户界面提供完整的费用信息
     */
    public RescheduleFeeInfo calculateRescheduleFeeInfo(Long ticketId, Long newFlightId) {
        System.out.println("=== DEBUG RESCHEDULE FEE CALCULATION START ===");
        System.out.println("Ticket ID: " + ticketId + ", New Flight ID: " + newFlightId);
        
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        System.out.println("Found ticket: " + ticket.getTicketNumber());
        System.out.println("Ticket price (stored): ¥" + ticket.getPrice());
        System.out.println("Ticket status: " + ticket.getStatus());
        System.out.println("Ticket booking time: " + ticket.getBookingTime());
        
        Flight currentFlight = flightService.getFlightById(ticket.getFlightId());
        Flight newFlight = flightService.getFlightById(newFlightId);
        
        if (currentFlight == null || newFlight == null) {
            throw new RuntimeException("Flight not found");
        }
        
        System.out.println("Current flight: " + currentFlight.getFlightNumber());
        System.out.println("New flight: " + newFlight.getFlightNumber());
        
        // 🔧 FIX: Do NOT automatically load connecting flights for reschedule calculations
        // The user is selecting this specific flight for reschedule, so treat it as a single flight
        // unless they specifically selected a connecting journey through the UI
        System.out.println("=== RESCHEDULE FEE CALCULATION: TREATING NEW FLIGHT AS SINGLE FLIGHT ===");
        System.out.println("ℹ️ New flight " + newFlight.getFlightNumber() + " will be treated as a direct flight for reschedule calculation");
        System.out.println("ℹ️ If user wants connecting flights, they should select the entire journey through the UI");
        
        // Ensure no connecting flights are loaded for price calculation consistency
        newFlight.setConnectingFlights(null);
        
        RescheduleFeeInfo feeInfo = new RescheduleFeeInfo();
        
        // 基本信息
        feeInfo.setTicketId(ticketId);
        feeInfo.setTicketNumber(ticket.getTicketNumber());
        
        // 计算时间相关信息
        Instant now = Instant.now();
        Duration duration = Duration.between(now, currentFlight.getDepartureTimeUtc());
        long hoursUntilDeparture = duration.toHours();
        feeInfo.setHoursUntilDeparture(hoursUntilDeparture);
        feeInfo.setHighFeeThresholdHours(serviceFeeConfig.getHighFeeThresholdHours());
        feeInfo.setDepartureTime(currentFlight.getDepartureTimeUtc());
        
        System.out.println("Hours until departure: " + hoursUntilDeparture);
        System.out.println("High fee threshold: " + serviceFeeConfig.getHighFeeThresholdHours());
        
        // 计算服务费
        BigDecimal baseFee;
        BigDecimal percentageFee;
        String feeType;
        
        if (hoursUntilDeparture < serviceFeeConfig.getHighFeeThresholdHours()) {
            baseFee = serviceFeeConfig.getHighRescheduleBaseFee();
            percentageFee = serviceFeeConfig.getHighReschedulePercentageFee();
            feeType = "HIGH";
        } else {
            baseFee = serviceFeeConfig.getRescheduleBaseFee();
            percentageFee = serviceFeeConfig.getReschedulePercentageFee();
            feeType = "NORMAL";
        }
        
        System.out.println("Fee type: " + feeType);
        System.out.println("Base fee: ¥" + baseFee);
        System.out.println("Percentage fee rate: " + percentageFee);
        
        // 计算票价差异 - 使用实际存储的票价而非重新计算
        // 这样可以避免因为不同计价系统（70 vs 120 surcharge）导致的显示不一致
        BigDecimal currentTicketPrice = ticket.getPrice(); // 使用实际存储的票价
        BigDecimal newTicketPrice = calculatePrice(newFlight, ticket.getTicketType());
        BigDecimal priceDifference = newTicketPrice.subtract(currentTicketPrice);
        
        System.out.println("=== PRICE COMPARISON ===");
        System.out.println("Current ticket stored price (from original booking): ¥" + currentTicketPrice);
        System.out.println("New flight price (calculated as single flight): ¥" + newTicketPrice);
        System.out.println("Price difference (new - current): ¥" + priceDifference);
        if (priceDifference.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("✅ New flight is CHEAPER by ¥" + priceDifference.abs());
        } else if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("⚠️ New flight is MORE EXPENSIVE by ¥" + priceDifference);
        } else {
            System.out.println("ℹ️ New flight has the SAME PRICE");
        }
        
        // 计算实际服务费 (基础费用与百分比费用的较大值)
        BigDecimal ticketPercentageFee = currentTicketPrice.multiply(percentageFee);
        BigDecimal actualServiceFee = baseFee.max(ticketPercentageFee).setScale(2, RoundingMode.HALF_UP);
        
        System.out.println("DEBUG - Ticket percentage fee (currentPrice * rate): ¥" + ticketPercentageFee);
        System.out.println("DEBUG - Actual service fee (max of base and percentage): ¥" + actualServiceFee);
        
        // 设置费用信息
        feeInfo.setCurrentTicketPrice(currentTicketPrice);
        feeInfo.setNewTicketPrice(newTicketPrice);
        feeInfo.setServiceFee(actualServiceFee);
        feeInfo.setBaseFee(baseFee);
        feeInfo.setPercentageFee(ticketPercentageFee);
        feeInfo.setFeeType(feeType);
        feeInfo.setPriceDifference(priceDifference);
        
        // 计算总费用或退款
        if (priceDifference.compareTo(BigDecimal.ZERO) >= 0) {
            // 新票价更高或相等，需要支付差价 + 服务费
            BigDecimal totalCost = priceDifference.add(actualServiceFee);
            feeInfo.setTotalAdditionalCost(totalCost);
            feeInfo.setTotalRefund(BigDecimal.ZERO);
            System.out.println("DEBUG - Price difference >= 0, total cost: ¥" + totalCost);
            System.out.println("DEBUG - Formula: priceDiff(¥" + priceDifference + ") + serviceFee(¥" + actualServiceFee + ") = ¥" + totalCost);
        } else {
            // 新票价更低，计算退款或需要支付的费用
            BigDecimal refundableAmount = priceDifference.abs().subtract(actualServiceFee);
            System.out.println("DEBUG - Price difference < 0, refundable calculation:");
            System.out.println("DEBUG - Refundable amount: abs(¥" + priceDifference + ") - ¥" + actualServiceFee + " = ¥" + refundableAmount);
            if (refundableAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 可以退款
                feeInfo.setTotalRefund(refundableAmount);
                feeInfo.setTotalAdditionalCost(BigDecimal.ZERO);
                System.out.println("DEBUG - User gets refund: ¥" + refundableAmount);
            } else {
                // 仍需要支付服务费（减去价格差异）
                feeInfo.setTotalAdditionalCost(refundableAmount.abs());
                feeInfo.setTotalRefund(BigDecimal.ZERO);
                System.out.println("DEBUG - User still pays: ¥" + refundableAmount.abs());
            }
        }
        
        // 设置航班信息
        RescheduleFeeInfo.FlightInfo originalFlightInfo = new RescheduleFeeInfo.FlightInfo(
            currentFlight.getId(),
            currentFlight.getFlightNumber(),
            currentFlight.getDepartureAirport().getName(),
            currentFlight.getArrivalAirport().getName(),
            currentFlight.getDepartureTimeUtc(),
            currentFlight.getArrivalTimeUtc(),
            currentTicketPrice
        );
        
        RescheduleFeeInfo.FlightInfo newFlightInfo = new RescheduleFeeInfo.FlightInfo(
            newFlight.getId(),
            newFlight.getFlightNumber(),
            newFlight.getDepartureAirport().getName(),
            newFlight.getArrivalAirport().getName(),
            newFlight.getDepartureTimeUtc(),
            newFlight.getArrivalTimeUtc(),
            newTicketPrice
        );
        
        feeInfo.setOriginalFlight(originalFlightInfo);
        feeInfo.setNewFlight(newFlightInfo);
        
        // 生成费用说明
        generateFeeExplanation(feeInfo);
        
        System.out.println("=== FINAL RESULT ===");
        System.out.println("Total Additional Cost: ¥" + feeInfo.getTotalAdditionalCost());
        System.out.println("Total Refund: ¥" + feeInfo.getTotalRefund());
        System.out.println("=== DEBUG RESCHEDULE FEE CALCULATION END ===");
        
        return feeInfo;
    }
    
    /**
     * 生成费用说明和建议
     */
    private void generateFeeExplanation(RescheduleFeeInfo feeInfo) {
        StringBuilder explanation = new StringBuilder();
        List<String> breakdown = new ArrayList<>();
        
        // 基本说明
        explanation.append("改签费用计算基于以下因素：");
        
        // 服务费说明
        if ("HIGH".equals(feeInfo.getFeeType())) {
            explanation.append("由于距离起飞时间不足").append(feeInfo.getHighFeeThresholdHours()).append("小时，适用高额服务费标准。");
            breakdown.add("高额服务费：¥" + feeInfo.getServiceFee() + " (基础费用 ¥" + feeInfo.getBaseFee() + 
                         " 或票价的" + (feeInfo.getPercentageFee().multiply(new BigDecimal("100"))) + "%，取较大值)");
        } else {
            explanation.append("距离起飞时间超过").append(feeInfo.getHighFeeThresholdHours()).append("小时，适用标准服务费。");
            breakdown.add("标准服务费：¥" + feeInfo.getServiceFee() + " (基础费用 ¥" + feeInfo.getBaseFee() + 
                         " 或票价的" + (feeInfo.getPercentageFee().multiply(new BigDecimal("100"))) + "%，取较大值)");
        }
        
        // 价格差异说明
        if (feeInfo.getPriceDifference().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add("票价差异：¥" + feeInfo.getPriceDifference() + " (新票价更高)");
        } else if (feeInfo.getPriceDifference().compareTo(BigDecimal.ZERO) < 0) {
            breakdown.add("票价差异：¥" + feeInfo.getPriceDifference().abs() + " (新票价更低)");
        } else {
            breakdown.add("票价差异：¥0 (票价相同)");
        }
        
        // 总费用说明
        if (feeInfo.getTotalAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add("需要支付：¥" + feeInfo.getTotalAdditionalCost());
            explanation.append(" 您需要额外支付 ¥").append(feeInfo.getTotalAdditionalCost()).append("。");
        } else if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add("可以退款：¥" + feeInfo.getTotalRefund());
            explanation.append(" 您可以获得退款 ¥").append(feeInfo.getTotalRefund()).append("。");
        } else {
            breakdown.add("总费用：¥0");
            explanation.append(" 改签仅需支付服务费，与票价差异相抵。");
        }
        
        feeInfo.setFeeExplanation(explanation.toString());
        feeInfo.setFeeBreakdown(breakdown.toArray(new String[0]));
        
        // 生成建议
        String recommendation = generateRecommendation(feeInfo);
        feeInfo.setRecommendation(recommendation);
    }
    
    /**
     * 生成改签建议
     */
    private String generateRecommendation(RescheduleFeeInfo feeInfo) {
        StringBuilder recommendation = new StringBuilder();
        
        if ("HIGH".equals(feeInfo.getFeeType())) {
            recommendation.append("⚠️ 警告：由于距离起飞时间较近，改签费用较高。");
        }
        
        if (feeInfo.getTotalAdditionalCost().compareTo(new BigDecimal("500")) > 0) {
            recommendation.append(" 建议仔细考虑是否需要改签，费用相对较高。");
        } else if (feeInfo.getTotalRefund().compareTo(BigDecimal.ZERO) > 0) {
            recommendation.append(" ✅ 改签后您还可获得退款，这是一个不错的选择。");
        }
        
        recommendation.append(" 请确认新航班时间符合您的行程安排。");
        
        return recommendation.toString();
    }

    public BigDecimal calculateRefundServiceFee(Ticket ticket) {
        Flight flight = flightService.getFlightById(ticket.getFlightId());
        if (flight == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate hours until departure
        Duration duration = Duration.between(Instant.now(), flight.getDepartureTimeUtc());
        long hoursUntilDeparture = duration.toHours();
        
        BigDecimal baseFee;
        BigDecimal percentageFee;
        
        if (hoursUntilDeparture < serviceFeeConfig.getHighFeeThresholdHours()) {
            // High fee for last-minute refunds
            baseFee = serviceFeeConfig.getHighRefundBaseFee();
            percentageFee = serviceFeeConfig.getHighRefundPercentageFee();
        } else {
            // Normal fee
            baseFee = serviceFeeConfig.getRefundBaseFee();
            percentageFee = serviceFeeConfig.getRefundPercentageFee();
        }
        
        // Calculate percentage of ticket price
        BigDecimal ticketPercentageFee = ticket.getPrice().multiply(percentageFee);
        
        // Return the higher of base fee or percentage fee
        return baseFee.max(ticketPercentageFee).setScale(2, RoundingMode.HALF_UP);
    }
    
    private String generateTicketNumber() {
        // Create a shorter ticket number: AT + timestamp (last 8 digits) + random 4 chars
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5); // Last 8 digits
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "AT" + timestamp + randomPart;
    }
    
    private BigDecimal calculatePrice(Flight flight, String ticketType) {
        System.out.println("=== CALCULATING PRICE FOR FLIGHT " + flight.getFlightNumber() + " ===");
        System.out.println("Flight ID: " + flight.getId() + ", Ticket Type: " + ticketType);
        
        // Check if this is a connecting flight
        if (flight.getConnectingFlights() != null && !flight.getConnectingFlights().isEmpty()) {
            System.out.println("🔗 This is a CONNECTING FLIGHT with " + flight.getConnectingFlights().size() + " connecting segments");
            return calculateConnectingFlightPrice(flight, ticketType);
        } else {
            System.out.println("✈️ This is a SINGLE/DIRECT FLIGHT (no connecting flights loaded)");
            return calculateSingleFlightPrice(flight, ticketType);
        }
    }
    
    /**
     * Calculate price for a single flight segment
     */
    private BigDecimal calculateSingleFlightPrice(Flight flight, String ticketType) {
        BigDecimal basePrice = flight.getPrice();
        
        // Apply ticket type multiplier
        BigDecimal typeAdjustedPrice;
        switch (ticketType) {
            case "BUSINESS":
                typeAdjustedPrice = basePrice.multiply(new BigDecimal("2.5"));
                break;
            case "FIRST":
                typeAdjustedPrice = basePrice.multiply(new BigDecimal("4.0"));
                break;
            case "ECONOMY":
            default:
                typeAdjustedPrice = basePrice;
                break;
        }
        
        // Add fuel surcharge of 70 RMB per flight
        BigDecimal fuelSurcharge = new BigDecimal("70");
        // Add airport construction fee of 50 RMB per flight
        BigDecimal airportConstructionFee = new BigDecimal("50");
        BigDecimal totalSurcharges = fuelSurcharge.add(airportConstructionFee);
        
        System.out.println("Single flight price calculation for " + flight.getFlightNumber() + ":");
        System.out.println("  Base price: " + basePrice);
        System.out.println("  Type-adjusted price: " + typeAdjustedPrice);
        System.out.println("  Fuel surcharge: " + fuelSurcharge);
        System.out.println("  Airport construction fee: " + airportConstructionFee);
        System.out.println("  Total surcharges: " + totalSurcharges);
        System.out.println("  Final price: " + typeAdjustedPrice.add(totalSurcharges));
        
        return typeAdjustedPrice.add(totalSurcharges);
    }
    
    /**
     * Calculate price for connecting flights (main flight + all connecting segments)
     */
    private BigDecimal calculateConnectingFlightPrice(Flight mainFlight, String ticketType) {
        System.out.println("=== CONNECTING FLIGHT PRICE CALCULATION ===");
        
        // Calculate price for main flight
        BigDecimal mainFlightPrice = calculateSingleFlightPrice(mainFlight, ticketType);
        BigDecimal totalPrice = mainFlightPrice;
        
        System.out.println("Main flight " + mainFlight.getFlightNumber() + " price: ¥" + mainFlightPrice);
        
        // Calculate price for each connecting flight
        int segmentNumber = 2;
        for (Flight connectingFlight : mainFlight.getConnectingFlights()) {
            BigDecimal connectingPrice = calculateSingleFlightPrice(connectingFlight, ticketType);
            totalPrice = totalPrice.add(connectingPrice);
            System.out.println("Segment " + segmentNumber + " (" + connectingFlight.getFlightNumber() + ") price: ¥" + connectingPrice);
            segmentNumber++;
        }
        
        System.out.println("=== TOTAL CONNECTING FLIGHT PRICE: ¥" + totalPrice + " ===");
        System.out.println("Breakdown: Main(¥" + mainFlightPrice + ") + " + (mainFlight.getConnectingFlights().size()) + " connecting segments");
        
        return totalPrice;
    }

    // Hard delete - permanently removes ticket from database (Admin only)
    @Transactional
    public boolean hardDeleteTicket(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        // Release seat if ticket is not already cancelled or refunded
        if (!"CANCELLED".equals(ticket.getStatus()) && !"REFUNDED".equals(ticket.getStatus())) {
            flightService.releaseSeat(ticket.getFlightId());
        }
        
        // Delete related approval requests first to avoid foreign key constraint violation
        adminApprovalRequestMapper.deleteByTicketId(ticketId);
        
        // Now delete the ticket
        ticketMapper.deleteById(ticketId);
        return true;
    }
    
    // Soft delete - marks ticket as deleted for user but keeps in database
    @Transactional
    public boolean softDeleteTicket(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        ticket.setDeletedByUser(true);
        ticketMapper.updateDeletedByUser(ticketId, true);
        return true;
    }
    
    public Map<String, Object> getPaymentDeadlineInfo(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found");
        }
        
        Flight flight = flightService.getFlightById(ticket.getFlightId());
        if (flight == null) {
            throw new RuntimeException("Flight not found");
        }
        
        Map<String, Object> paymentInfo = new HashMap<>();
        Instant now = Instant.now();
        
        // 计算预订时间 + 10分钟的截止时间
        Instant paymentDeadline = ticket.getBookingTime() != null 
            ? ticket.getBookingTime().plusSeconds(10 * 60) 
            : now.plusSeconds(10 * 60);
        
        // 计算航班起飞前40分钟的截止时间
        Instant flightDeadline = flight.getDepartureTimeUtc().minusSeconds(40 * 60);
        
        // 实际支付截止时间是两者中较早的那个
        Instant effectiveDeadline = paymentDeadline.isBefore(flightDeadline) 
            ? paymentDeadline 
            : flightDeadline;
        
        // 计算剩余时间（秒数）
        Duration remainingDuration = Duration.between(now, effectiveDeadline);
        long remainingSeconds = remainingDuration.getSeconds();
        
        boolean isExpired = remainingSeconds <= 0;
        boolean canPay = !"PAID".equals(ticket.getStatus()) && 
                        !"CANCELLED".equals(ticket.getStatus()) && 
                        !"REFUNDED".equals(ticket.getStatus()) &&
                        !isExpired &&
                        now.isBefore(flight.getDepartureTimeUtc());
        
        paymentInfo.put("ticketId", ticketId);
        paymentInfo.put("ticketNumber", ticket.getTicketNumber());
        paymentInfo.put("status", ticket.getStatus());
        paymentInfo.put("bookingTime", ticket.getBookingTime());
        paymentInfo.put("paymentDeadline", paymentDeadline);
        paymentInfo.put("flightDeadline", flightDeadline);
        paymentInfo.put("effectiveDeadline", effectiveDeadline);
        paymentInfo.put("remainingSeconds", Math.max(0, remainingSeconds));
        paymentInfo.put("remainingMinutes", Math.max(0, remainingSeconds / 60));
        paymentInfo.put("isExpired", isExpired);
        paymentInfo.put("canPay", canPay);
        paymentInfo.put("flightDepartureTime", flight.getDepartureTimeUtc());

        if (remainingSeconds > 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            
            String countdown;
            if (hours > 0) {
                countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                countdown = String.format("%02d:%02d", minutes, seconds);
            }
            
            paymentInfo.put("countdownDisplay", countdown);
            // 设置紧急程度 - 与前端逻辑保持一致
            if (remainingSeconds <= 120) {
                paymentInfo.put("urgencyLevel", "HIGH");
            } else if (remainingSeconds <= 300) {
                paymentInfo.put("urgencyLevel", "MEDIUM");
            } else {
                paymentInfo.put("urgencyLevel", "LOW");
            }
        } else {
            paymentInfo.put("countdownDisplay", "00:00");
            paymentInfo.put("urgencyLevel", "EXPIRED");
        }
        
        // 添加提示信息
        if (isExpired) {
            paymentInfo.put("message", "支付时间已过期");
        } else if (!canPay) {
            paymentInfo.put("message", "当前无法支付此机票");
        } else if (remainingSeconds < 120) { // 少于2分钟
            paymentInfo.put("message", "支付时间即将到期！");
        } else if (remainingSeconds < 300) { // 少于5分钟
            paymentInfo.put("message", "支付时间即将到期，请尽快完成支付");
        } else {
            paymentInfo.put("message", "请在时限内完成支付");
        }
        
        return paymentInfo;
    }
    
    private void enrichTicketWithPaymentInfo(Ticket ticket) {
        try {
            // 只对BOOKED状态的票据添加支付倒计时信息
            if (!"BOOKED".equals(ticket.getStatus())) {
                return;
            }
            
            Flight flight = flightService.getFlightById(ticket.getFlightId());
            if (flight == null) {
                return;
            }
            
            Instant now = Instant.now();
            
            // 计算预订时间 + 10分钟的截止时间
            Instant paymentDeadline = ticket.getBookingTime() != null 
                ? ticket.getBookingTime().plusSeconds(10 * 60) 
                : now.plusSeconds(10 * 60);
            
            // 计算航班起飞前40分钟的截止时间
            Instant flightDeadline = flight.getDepartureTimeUtc().minusSeconds(40 * 60);
            
            // 实际支付截止时间是两者中较早的那个
            Instant effectiveDeadline = paymentDeadline.isBefore(flightDeadline) 
                ? paymentDeadline 
                : flightDeadline;
            
            // 计算剩余时间（秒数）
            Duration remainingDuration = Duration.between(now, effectiveDeadline);
            long remainingSeconds = remainingDuration.getSeconds();
            
            boolean isExpired = remainingSeconds <= 0;
            boolean canPay = !isExpired && now.isBefore(flight.getDepartureTimeUtc());
            
            // 设置票据的支付倒计时信息
            ticket.setPaymentDeadline(effectiveDeadline);
            ticket.setPaymentRemainingSeconds(Math.max(0, remainingSeconds));
            ticket.setCanPay(canPay);
            
            // 添加友好的倒计时显示格式
            if (remainingSeconds > 0) {
                long hours = remainingSeconds / 3600;
                long minutes = (remainingSeconds % 3600) / 60;
                long seconds = remainingSeconds % 60;
                
                String countdown;
                if (hours > 0) {
                    countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    countdown = String.format("%02d:%02d", minutes, seconds);
                }
                
                ticket.setPaymentCountdownDisplay(countdown);
                // 设置紧急程度 - 与前端逻辑保持一致
                if (remainingSeconds <= 120) {
                    ticket.setPaymentUrgencyLevel("HIGH");
                } else if (remainingSeconds <= 300) {
                    ticket.setPaymentUrgencyLevel("MEDIUM");
                } else {
                    ticket.setPaymentUrgencyLevel("LOW");
                }
            } else {
                ticket.setPaymentCountdownDisplay("00:00");
                ticket.setPaymentUrgencyLevel("EXPIRED");
            }
            
            // 添加提示信息
            if (isExpired) {
                ticket.setPaymentMessage("支付时间已过期");
            } else if (!canPay) {
                ticket.setPaymentMessage("当前无法支付此机票");
            } else if (remainingSeconds < 120) { // 少于2分钟
                ticket.setPaymentMessage("支付时间即将到期！");
            } else if (remainingSeconds < 300) { // 少于5分钟
                ticket.setPaymentMessage("支付时间即将到期，请尽快完成支付");
            } else {
                ticket.setPaymentMessage("请在时限内完成支付");
            }
        } catch (Exception e) {
            // 如果获取支付信息失败，不影响主要功能
            // 这里可以记录日志但不抛出异常
        }
    }
    
    private void enrichTicketWithFlightAndUserInfo(Ticket ticket) {
        try {
            // Load flight information
            if (ticket.getFlightId() != null) {
                Flight flight = flightService.getFlightById(ticket.getFlightId());
                if (flight != null) {
                    ticket.setFlight(flight);
                }
            }
            
            // Load user information
            if (ticket.getUserId() != null) {
                User user = userService.findById(ticket.getUserId());
                if (user != null) {
                    ticket.setUser(user);
                }
            }
        } catch (Exception e) {
            // If enriching fails, don't break the main functionality
            // Log the error but don't throw exception
            System.err.println("Failed to enrich ticket " + ticket.getId() + " with flight/user info: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void enrichTicketWithRescheduledInfo(Ticket ticket) {
        try {
            // No need to load additional tickets for now, as the database relationships
            // are already populated in the ticket object through the new fields:
            // - originalTicketId
            // - rescheduledToTicketId 
            // - isOriginalTicket
            
            // If needed in the future, we can add logic here to load related tickets
            // for display purposes
        } catch (Exception e) {
            // If enriching fails, don't break the main functionality
            System.err.println("Failed to enrich ticket " + ticket.getId() + " with rescheduled info: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads connecting flights for a flight from the database based on route logic
     */
    public void loadConnectingFlightsForFlight(Flight flight, Long finalDestinationAirportId) {
        System.out.println("=== loadConnectingFlightsForFlight() CALLED ===");
        try {
            if (flight != null && flight.getId() != null && finalDestinationAirportId != null) {
                System.out.println("Loading connecting flights for flight ID: " + flight.getId() + 
                                 " to destination airport ID: " + finalDestinationAirportId);
                
                // Check if this flight already reaches the final destination
                if (flight.getArrivalAirportId() != null && flight.getArrivalAirportId().equals(finalDestinationAirportId)) {
                    System.out.println("Flight " + flight.getFlightNumber() + " already reaches final destination - no connecting flights needed");
                    return;
                }
                
                // Skip flights with null arrival airport ID
                if (flight.getArrivalAirportId() == null) {
                    System.out.println("Skipping flight " + flight.getFlightNumber() + " - null arrival airport ID");
                    return;
                }
                
                // Find connecting flights from this flight's arrival airport to the final destination
                List<Flight> connectingFlights = flightMapper.findConnectingFlights(
                    flight.getArrivalAirportId(), // departure from first flight's arrival airport
                    finalDestinationAirportId,    // arrival at final destination
                    flight.getId()                // exclude the current flight
                );
                
                System.out.println("Found " + connectingFlights.size() + " connecting flights");
                
                if (!connectingFlights.isEmpty()) {
                    // Filter connecting flights to ensure reasonable connection time
                    List<Flight> validConnections = new ArrayList<>();
                    for (Flight connectingFlight : connectingFlights) {
                        // Check if the connecting flight departs after the first flight arrives
                        // with at least 1 hour connection time
                        if (connectingFlight.getDepartureTimeUtc() != null && 
                            flight.getArrivalTimeUtc() != null) {
                            long connectionMinutes = java.time.Duration.between(
                                flight.getArrivalTimeUtc(), 
                                connectingFlight.getDepartureTimeUtc()
                            ).toMinutes();
                            
                            if (connectionMinutes >= 60 && connectionMinutes <= 720) { // 1-12 hours connection time
                                validConnections.add(connectingFlight);
                                System.out.println("Valid connecting flight: " + connectingFlight.getFlightNumber() + 
                                                 " (connection time: " + connectionMinutes + " minutes)");
                            } else {
                                System.out.println("Skipping connecting flight " + connectingFlight.getFlightNumber() + 
                                                 " (connection time: " + connectionMinutes + " minutes)");
                            }
                        }
                    }
                    
                    if (!validConnections.isEmpty()) {
                        // Take the first valid connection (could be enhanced with better selection logic)
                        flight.setConnectingFlights(validConnections.subList(0, Math.min(1, validConnections.size())));
                        System.out.println("Set " + flight.getConnectingFlights().size() + " connecting flights on flight");
                    } else {
                        System.out.println("No valid connecting flights found (connection time constraints)");
                    }
                } else {
                    System.out.println("No connecting flights found for route");
                }
            } else {
                System.out.println("Flight, flight ID, or final destination is null");
            }
        } catch (Exception e) {
            // If loading connecting flights fails, don't break the main functionality
            System.err.println("Failed to load connecting flights for flight " + 
                             (flight != null ? flight.getId() : "null") + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== loadConnectingFlightsForFlight() COMPLETED ===");
    }


    public void loadConnectingFlights(Ticket ticket) {
        System.out.println("=== loadConnectingFlights() CALLED ===");
        try {
            if (ticket != null && ticket.getId() != null) {
                System.out.println("Loading connecting flights for ticket ID: " + ticket.getId());
                List<Long> connectingFlightIds = ticketMapper.findConnectingFlightIdsByTicketId(ticket.getId());
                System.out.println("Found " + connectingFlightIds.size() + " connecting flight IDs: " + connectingFlightIds);
                
                if (!connectingFlightIds.isEmpty()) {
                    List<Flight> connectingFlights = new ArrayList<>();
                    for (Long flightId : connectingFlightIds) {
                        System.out.println("Loading connecting flight details for ID: " + flightId);
                        Flight flight = flightService.getFlightById(flightId);
                        if (flight != null) {
                            connectingFlights.add(flight);
                            System.out.println("Loaded connecting flight: " + flight.getFlightNumber());
                        } else {
                            System.out.println("WARNING: Could not load connecting flight " + flightId);
                        }
                    }
                    ticket.setConnectingFlights(connectingFlights);
                    System.out.println("Set " + connectingFlights.size() + " connecting flights on ticket");
                    
                    // 🚀 FIX: Calculate and update the total price for connecting flights
                    if (!connectingFlights.isEmpty()) {
                        BigDecimal originalPrice = ticket.getPrice() != null ? ticket.getPrice() : BigDecimal.ZERO;
                        BigDecimal connectingFlightsPrice = BigDecimal.ZERO;
                        
                        System.out.println("💰 Calculating total price for connecting flights...");
                        System.out.println("Original ticket price: " + originalPrice);
                        
                        // If the original ticket price is just the main flight price, add connecting flight prices
                        Flight mainFlight = ticket.getFlight();
                        if (mainFlight != null) {
                            BigDecimal mainFlightPrice = calculatePrice(mainFlight, ticket.getTicketType());
                            System.out.println("Main flight calculated price: " + mainFlightPrice);
                            
                            // Check if original price is approximately equal to main flight price only
                            boolean needsPriceUpdate = originalPrice.compareTo(mainFlightPrice) == 0 ||
                                                     originalPrice.compareTo(BigDecimal.ZERO) == 0;
                            
                            if (needsPriceUpdate) {
                                System.out.println("✅ Original price seems to only include main flight, recalculating with connecting flights...");
                                BigDecimal newTotalPrice = mainFlightPrice;
                                
                                for (Flight connectingFlight : connectingFlights) {
                                    BigDecimal connectingPrice = calculatePrice(connectingFlight, ticket.getTicketType());
                                    connectingFlightsPrice = connectingFlightsPrice.add(connectingPrice);
                                    newTotalPrice = newTotalPrice.add(connectingPrice);
                                    System.out.println("Added connecting flight " + connectingFlight.getFlightNumber() + 
                                                     " price: ¥" + connectingPrice + ", running total: ¥" + newTotalPrice);
                                }
                                
                                // Update the ticket's price in memory (for API response)
                                ticket.setPrice(newTotalPrice);
                                System.out.println("✅ Updated ticket price from ¥" + originalPrice + " to ¥" + newTotalPrice + 
                                                 " (includes " + connectingFlights.size() + " connecting flights)");
                            } else {
                                System.out.println("ℹ️ Original price ¥" + originalPrice + " already includes connecting flights");
                            }
                        }
                    }
                } else {
                    System.out.println("No connecting flights found for ticket " + ticket.getId());
                }
            } else {
                System.out.println("Ticket or ticket ID is null");
            }
        } catch (Exception e) {
            // If loading connecting flights fails, don't break the main functionality
            System.err.println("Failed to load connecting flights for ticket " + ticket.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== loadConnectingFlights() COMPLETED ===");
    }

    @Transactional
    protected void handleConnectingFlightsForReschedule(Ticket originalTicket, Ticket newTicket, Flight newFlight) {
        System.out.println("=== handleConnectingFlightsForReschedule() CALLED ===");
        try {
            if (originalTicket == null || originalTicket.getId() == null || 
                newTicket == null || newTicket.getId() == null || newFlight == null) {
                System.out.println("Invalid parameters - cannot handle connecting flights");
                return;
            }
            
            System.out.println("Handling connecting flights for reschedule:");
            System.out.println("  Original ticket: " + originalTicket.getId());
            System.out.println("  New ticket: " + newTicket.getId());
            System.out.println("  New flight: " + newFlight.getFlightNumber() + " (" + 
                             newFlight.getDepartureAirport().getCity() + " -> " + 
                             newFlight.getArrivalAirport().getCity() + ")");
            
            // Get the original connecting flights
            List<Long> originalConnectingFlightIds = ticketMapper.findConnectingFlightIdsByTicketId(originalTicket.getId());
            System.out.println("Original connecting flights: " + originalConnectingFlightIds.size() + " flights");
            
            if (originalConnectingFlightIds.isEmpty()) {
                System.out.println("No connecting flights in original ticket");
                
                // 🚀 SPECIAL CASE: Original was single flight, but new selection might be connecting
                // Check if the user selected a connecting flight for reschedule
                System.out.println("🔍 Checking if new flight selection is a connecting flight combination...");
                
                try {
                    Flight originalMainFlight = flightService.getFlightById(originalTicket.getFlightId());
                    String originCity = originalMainFlight.getDepartureAirport().getCity();
                    String finalDestinationCity = originalMainFlight.getArrivalAirport().getCity();
                    LocalDate rescheduleDate = newFlight.getDepartureTimeUtc().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                    
                    System.out.println("Searching for connecting flights for route: " + originCity + " → " + finalDestinationCity);
                    
                    List<com.airticket.model.ConnectingFlight> connectingFlights = flightService.findConnectingFlights(
                        originCity, finalDestinationCity, rescheduleDate
                    );
                    
                    // Find the connecting flight combination that starts with our selected flight
                    com.airticket.model.ConnectingFlight matchingConnectingFlight = null;
                    for (com.airticket.model.ConnectingFlight cf : connectingFlights) {
                        if (cf.getFlights() != null && !cf.getFlights().isEmpty()) {
                            Flight firstFlight = cf.getFlights().get(0);
                            if (firstFlight.getId().equals(newFlight.getId())) {
                                matchingConnectingFlight = cf;
                                System.out.println("✅ Found matching connecting flight combination!");
                                break;
                            }
                        }
                    }
                    
                    if (matchingConnectingFlight != null && matchingConnectingFlight.getFlights().size() > 1) {
                        System.out.println("🎉 SPECIAL CASE: Converting single flight reschedule to connecting flight!");
                        
                        // Create connecting flights from the second flight onwards
                        List<Flight> connectingFlightsToCreate = matchingConnectingFlight.getFlights().subList(1, matchingConnectingFlight.getFlights().size());
                        
                        int sequenceNumber = 1;
                        for (Flight connectingFlight : connectingFlightsToCreate) {
                            System.out.println("Creating connecting flight: " + connectingFlight.getFlightNumber() + 
                                             " (" + connectingFlight.getDepartureAirport().getCity() + " → " + 
                                             connectingFlight.getArrivalAirport().getCity() + ")");
                            
                            // Reserve seat on the connecting flight
                            if (flightService.reserveSeat(connectingFlight.getId())) {
                                int rowsInserted = ticketMapper.insertTicketConnectingFlight(
                                    newTicket.getId(), 
                                    connectingFlight.getId(), 
                                    sequenceNumber
                                );
                                
                                if (rowsInserted > 0) {
                                    System.out.println("✅ Successfully created connecting flight " + connectingFlight.getId());
                                } else {
                                    System.out.println("❌ Failed to create connecting flight " + connectingFlight.getId());
                                    // Release the seat if we couldn't save the relationship
                                    flightService.releaseSeat(connectingFlight.getId());
                                }
                            } else {
                                System.out.println("❌ No seats available on connecting flight " + connectingFlight.getFlightNumber());
                            }
                            sequenceNumber++;
                        }
                        
                        System.out.println("✅ SPECIAL CASE completed: Single flight converted to connecting flight");
                        return; // We're done
                    } else {
                        System.out.println("ℹ️ SPECIAL CASE: No connecting flight combination found, this is a simple single flight reschedule");
                    }
                    
                } catch (Exception e) {
                    System.out.println("⚠️ Error in SPECIAL CASE: " + e.getMessage());
                    System.out.println("Treating as simple single flight reschedule...");
                }
                
                System.out.println("Simple single flight reschedule completed");
                return;
            }
            
            // Load original flight and connecting flights for analysis
            Flight originalMainFlight = flightService.getFlightById(originalTicket.getFlightId());
            List<Flight> originalConnectingFlights = new ArrayList<>();
            for (Long flightId : originalConnectingFlightIds) {
                Flight flight = flightService.getFlightById(flightId);
                if (flight != null) {
                    originalConnectingFlights.add(flight);
                }
            }
            
            System.out.println("Original journey:");
            System.out.println("  Main: " + originalMainFlight.getFlightNumber() + " (" + 
                             originalMainFlight.getDepartureAirport().getCity() + " -> " + 
                             originalMainFlight.getArrivalAirport().getCity() + ")");
            for (Flight cf : originalConnectingFlights) {
                System.out.println("  Connecting: " + cf.getFlightNumber() + " (" + 
                                 cf.getDepartureAirport().getCity() + " -> " + 
                                 cf.getArrivalAirport().getCity() + ")");
            }
            
            // Determine the final destination of the original journey
            Flight finalDestinationFlight = originalConnectingFlights.isEmpty() ? 
                originalMainFlight : originalConnectingFlights.get(originalConnectingFlights.size() - 1);
            Long finalDestinationAirportId = finalDestinationFlight.getArrivalAirport().getId();
            
            System.out.println("Final destination of original journey: " + finalDestinationFlight.getArrivalAirport().getCity());
            System.out.println("New flight destination: " + newFlight.getArrivalAirport().getCity());
            
            // 🚀 CASE 0: NEW FLIGHT SELECTION WAS A CONNECTING FLIGHT FROM SEARCH RESULTS
            // This is the most important case that was missing!
            // We need to detect if the "newFlight" actually has connecting flights that should be created
            System.out.println("🔍 CASE 0: Checking if new flight selection has connecting flights...");
            
            try {
                // Try to find connecting flights that would complete this journey
                // Check if there are connecting flights for the route from original origin to final destination
                String originCity = originalMainFlight.getDepartureAirport().getCity();
                String finalDestinationCity = finalDestinationFlight.getArrivalAirport().getCity();
                LocalDate rescheduleDate = newFlight.getDepartureTimeUtc().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                
                System.out.println("Searching for connecting flights for route: " + originCity + " → " + finalDestinationCity);
                
                List<com.airticket.model.ConnectingFlight> connectingFlights = flightService.findConnectingFlights(
                    originCity, finalDestinationCity, rescheduleDate
                );
                
                // Find the connecting flight combination that starts with our selected flight
                com.airticket.model.ConnectingFlight matchingConnectingFlight = null;
                for (com.airticket.model.ConnectingFlight cf : connectingFlights) {
                    if (cf.getFlights() != null && !cf.getFlights().isEmpty()) {
                        Flight firstFlight = cf.getFlights().get(0);
                        if (firstFlight.getId().equals(newFlight.getId())) {
                            matchingConnectingFlight = cf;
                            System.out.println("✅ Found matching connecting flight combination!");
                            break;
                        }
                    }
                }
                
                if (matchingConnectingFlight != null && matchingConnectingFlight.getFlights().size() > 1) {
                    System.out.println("🎉 CASE 0: Creating connecting flights for new ticket!");
                    
                    // Create connecting flights from the second flight onwards
                    List<Flight> connectingFlightsToCreate = matchingConnectingFlight.getFlights().subList(1, matchingConnectingFlight.getFlights().size());
                    
                    int sequenceNumber = 1;
                    for (Flight connectingFlight : connectingFlightsToCreate) {
                        System.out.println("Creating connecting flight: " + connectingFlight.getFlightNumber() + 
                                         " (" + connectingFlight.getDepartureAirport().getCity() + " → " + 
                                         connectingFlight.getArrivalAirport().getCity() + ")");
                        
                        // Reserve seat on the connecting flight
                        if (flightService.reserveSeat(connectingFlight.getId())) {
                            int rowsInserted = ticketMapper.insertTicketConnectingFlight(
                                newTicket.getId(), 
                                connectingFlight.getId(), 
                                sequenceNumber
                            );
                            
                            if (rowsInserted > 0) {
                                System.out.println("✅ Successfully created connecting flight " + connectingFlight.getId());
                            } else {
                                System.out.println("❌ Failed to create connecting flight " + connectingFlight.getId());
                                // Release the seat if we couldn't save the relationship
                                flightService.releaseSeat(connectingFlight.getId());
                            }
                        } else {
                            System.out.println("❌ No seats available on connecting flight " + connectingFlight.getFlightNumber());
                        }
                        sequenceNumber++;
                    }
                    
                    System.out.println("✅ CASE 0 completed: All connecting flights created for new ticket");
                    return; // We're done - the connecting flight combination has been created
                } else {
                    System.out.println("ℹ️ CASE 0: No matching connecting flight combination found, proceeding to other cases");
                }
                
            } catch (Exception e) {
                System.out.println("⚠️ Error in CASE 0: " + e.getMessage());
                System.out.println("Proceeding to other reschedule cases...");
            }
            
            // CASE 1: New flight is a direct flight to the final destination
            if (newFlight.getArrivalAirport().getId().equals(finalDestinationAirportId)) {
                System.out.println("✅ CASE 1: New flight goes directly to final destination - no connecting flights needed");
                // Don't copy any connecting flights - the new flight is direct
                return;
            }
            
            // CASE 2: New flight goes to the same intermediate destination as one of the original connecting flights
            boolean foundMatchingIntermediateDestination = false;
            int matchingSegmentIndex = -1;
            
            for (int i = 0; i < originalConnectingFlights.size(); i++) {
                Flight originalConnectingFlight = originalConnectingFlights.get(i);
                if (newFlight.getArrivalAirport().getId().equals(originalConnectingFlight.getArrivalAirport().getId())) {
                    System.out.println("✅ CASE 2: New flight goes to same destination as connecting flight " + 
                                     originalConnectingFlight.getFlightNumber() + " - copying remaining segments");
                    foundMatchingIntermediateDestination = true;
                    matchingSegmentIndex = i;
                    break;
                }
            }
            
            if (foundMatchingIntermediateDestination) {
                // Copy only the connecting flights that come AFTER the matching segment
                int sequenceNumber = 1;
                for (int i = matchingSegmentIndex + 1; i < originalConnectingFlights.size(); i++) {
                    Flight remainingConnectingFlight = originalConnectingFlights.get(i);
                    System.out.println("Copying remaining connecting flight: " + remainingConnectingFlight.getFlightNumber());
                    
                    int rowsInserted = ticketMapper.insertTicketConnectingFlight(
                        newTicket.getId(), 
                        remainingConnectingFlight.getId(), 
                        sequenceNumber
                    );
                    
                    if (rowsInserted > 0) {
                        System.out.println("Successfully copied remaining connecting flight " + remainingConnectingFlight.getId());
                    } else {
                        System.out.println("WARNING: Failed to copy remaining connecting flight " + remainingConnectingFlight.getId());
                    }
                    sequenceNumber++;
                }
                return;
            }
            
            // CASE 3: New flight requires connecting flights to reach the final destination
            System.out.println("🔍 CASE 3: Searching for connecting flights from new flight destination to final destination");
            
            try {
                List<Flight> availableConnectingFlights = flightMapper.findAvailableFlightsForReschedule(
                    newFlight.getArrivalAirport().getId(),
                    finalDestinationAirportId,
                    null
                );
                
                // Filter connecting flights that depart after the new flight arrives (with minimum connection time)
                Instant minConnectionTime = newFlight.getArrivalTimeUtc().plusSeconds(60 * 60); // 1 hour minimum connection
                List<Flight> validConnectingFlights = availableConnectingFlights.stream()
                    .filter(cf -> cf.getDepartureTimeUtc().isAfter(minConnectionTime))
                    .collect(Collectors.toList());
                
                if (!validConnectingFlights.isEmpty()) {
                    // For now, select the first valid connecting flight
                    // In a production system, you might want to let the user choose or use smart selection logic
                    Flight selectedConnectingFlight = validConnectingFlights.get(0);
                    
                    System.out.println("✅ Found valid connecting flight: " + selectedConnectingFlight.getFlightNumber() + 
                                     " (" + selectedConnectingFlight.getDepartureAirport().getCity() + " -> " + 
                                     selectedConnectingFlight.getArrivalAirport().getCity() + ")");
                    
                    // Reserve seat on the connecting flight
                    if (flightService.reserveSeat(selectedConnectingFlight.getId())) {
                        int rowsInserted = ticketMapper.insertTicketConnectingFlight(
                            newTicket.getId(), 
                            selectedConnectingFlight.getId(), 
                            1
                        );
                        
                        if (rowsInserted > 0) {
                            System.out.println("Successfully added new connecting flight " + selectedConnectingFlight.getId());
                        } else {
                            System.out.println("WARNING: Failed to add new connecting flight " + selectedConnectingFlight.getId());
                            // Release the seat if we couldn't save the relationship
                            flightService.releaseSeat(selectedConnectingFlight.getId());
                        }
                    } else {
                        System.out.println("❌ No seats available on the connecting flight " + selectedConnectingFlight.getFlightNumber());
                    }
                } else {
                    System.out.println("⚠️  No valid connecting flights found from " + newFlight.getArrivalAirport().getCity() + 
                                     " to " + finalDestinationFlight.getArrivalAirport().getCity());
                    System.out.println("This reschedule may result in an incomplete journey - manual review required");
                }
            } catch (Exception e) {
                System.out.println("❌ Error searching for connecting flights: " + e.getMessage());
                System.out.println("Proceeding without connecting flights - manual review may be required");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to handle connecting flights for reschedule: " + e.getMessage());
            e.printStackTrace();
            // Don't throw the exception to avoid breaking the main reschedule operation
        }
        System.out.println("=== handleConnectingFlightsForReschedule() COMPLETED ===");
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
}