package com.airticket.service;

import com.airticket.mapper.TicketMapper;
import com.airticket.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

@Service
public class TicketExpirationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketExpirationService.class);
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private FlightService flightService;
    
    @Scheduled(fixedRate = 30000) 
    @Transactional
    public void cancelExpiredTickets() {
        try {
            Instant now = Instant.now();
            
            List<Ticket> bookedTickets = ticketMapper.findExpiredBookedTickets(now);
            
            int canceledCount = 0;
            for (Ticket ticket : bookedTickets) {
                try {
                    if (isTicketExpired(ticket, now)) {
                        
                        flightService.releaseSeat(ticket.getFlightId());
                        
                        
                        ticket.setStatus("CANCELLED");
                        ticketMapper.updateStatus(ticket);
                        
                        canceledCount++;
                        logger.info("Auto-cancelled expired ticket: {} for user: {}", 
                                   ticket.getTicketNumber(), ticket.getUserId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to cancel expired ticket: " + ticket.getTicketNumber(), e);
                }
            }
            
            if (canceledCount > 0) {
                logger.info("Auto-cancelled {} expired tickets", canceledCount);
            }
            
        } catch (Exception e) {
            logger.error("Error in ticket expiration job", e);
        }
    }
    
    private boolean isTicketExpired(Ticket ticket, Instant now) {
        if (ticket.getBookingTime() == null) {
            return false;
        }
        
        
        if ("PENDING_RESCHEDULE".equals(ticket.getStatus())) {
            return false;
        }
        
        
        Instant paymentDeadline = ticket.getBookingTime().plusSeconds(10 * 60);
        
        
        if (ticket.getFlight() != null && ticket.getFlight().getDepartureTimeUtc() != null) {
            Instant flightDeadline = ticket.getFlight().getDepartureTimeUtc().minusSeconds(40 * 60);
            
            
            if (flightDeadline.isBefore(paymentDeadline)) {
                paymentDeadline = flightDeadline;
            }
        }
        
        return now.isAfter(paymentDeadline);
    }
}