package com.airticket.service;

import com.airticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private FlightMapper flightMapper;
    
    @Autowired
    private AirlineMapper airlineMapper;
    @Autowired
    private AdminApprovalRequestMapper adminApprovalRequestMapper;
    
    @Autowired
    private PaymentMapper paymentMapper;

    @Transactional
    public void resetEntireDatabase() {
        com.airticket.util.AppLog.debug("Starting complete database reset...");
        adminApprovalRequestMapper.deleteAll();
        paymentMapper.deleteAll();
        ticketMapper.deleteAll();
        flightMapper.deleteAll();
        airlineMapper.deleteAll();
        userMapper.deleteAllExceptAdmin();
        com.airticket.util.AppLog.debug("Complete database reset completed successfully");
    }

    @Transactional
    public void resetUserData() {
        com.airticket.util.AppLog.debug("Starting user data reset...");

        adminApprovalRequestMapper.deleteAll();
        paymentMapper.deleteAll();
        ticketMapper.deleteAll();
        userMapper.deleteAllExceptAdmin();
        
        com.airticket.util.AppLog.debug("User data reset completed successfully");
    }


    @Transactional
    public void resetTicketData() {
        com.airticket.util.AppLog.debug("Starting ticket data reset...");

        adminApprovalRequestMapper.deleteAll();
        paymentMapper.deleteAll();
        ticketMapper.deleteAll();
        flightMapper.resetAvailableSeats();
        
        com.airticket.util.AppLog.debug("Ticket data reset completed successfully");
    }

    @Transactional
    public void resetFlightData() {
        com.airticket.util.AppLog.debug("Starting flight data reset...");

        adminApprovalRequestMapper.deleteAll();
        paymentMapper.deleteAll();
        ticketMapper.deleteAll();
        flightMapper.deleteAll();
        
        com.airticket.util.AppLog.debug("Flight data reset completed successfully");
    }

    @Transactional
    public void resetAirlineData() {
        com.airticket.util.AppLog.debug("Starting airline data reset...");

        adminApprovalRequestMapper.deleteAll();
        paymentMapper.deleteAll();
        ticketMapper.deleteAll();
        flightMapper.deleteAll();
        airlineMapper.deleteAll();
        
        com.airticket.util.AppLog.debug("Airline data reset completed successfully");
    }

    public ResetStatistics getResetStatistics() {
        ResetStatistics stats = new ResetStatistics();
        
        try {
            var users = userMapper.findAll();
            stats.setUserCount(users != null ? users.size() : 0);
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Error getting user count: " + e.getMessage());
            stats.setUserCount(0);
        }
        
        try {
            var tickets = ticketMapper.findAll();
            stats.setTicketCount(tickets != null ? tickets.size() : 0);
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Error getting ticket count: " + e.getMessage());
            stats.setTicketCount(0);
        }
        
        try {
            var flights = flightMapper.findAll();
            stats.setFlightCount(flights != null ? flights.size() : 0);
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Error getting flight count: " + e.getMessage());
            stats.setFlightCount(0);
        }
        
        try {
            var airlines = airlineMapper.findAll();
            stats.setAirlineCount(airlines != null ? airlines.size() : 0);
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Error getting airline count: " + e.getMessage());
            stats.setAirlineCount(0);
        }
        
        try {
            var approvals = adminApprovalRequestMapper.findAll();
            stats.setApprovalRequestCount(approvals != null ? approvals.size() : 0);
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Error getting approval request count: " + e.getMessage());
            stats.setApprovalRequestCount(0);
        }
        
        return stats;
    }

    public static class ResetStatistics {
        private int userCount;
        private int ticketCount;
        private int flightCount;
        private int airlineCount;
        private int approvalRequestCount;

        
        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            this.userCount = userCount;
        }

        public int getTicketCount() {
            return ticketCount;
        }

        public void setTicketCount(int ticketCount) {
            this.ticketCount = ticketCount;
        }

        public int getFlightCount() {
            return flightCount;
        }

        public void setFlightCount(int flightCount) {
            this.flightCount = flightCount;
        }

        public int getAirlineCount() {
            return airlineCount;
        }

        public void setAirlineCount(int airlineCount) {
            this.airlineCount = airlineCount;
        }

        public int getApprovalRequestCount() {
            return approvalRequestCount;
        }

        public void setApprovalRequestCount(int approvalRequestCount) {
            this.approvalRequestCount = approvalRequestCount;
        }

        @Override
        public String toString() {
            return "ResetStatistics{" +
                    "userCount=" + userCount +
                    ", ticketCount=" + ticketCount +
                    ", flightCount=" + flightCount +
                    ", airlineCount=" + airlineCount +
                    ", approvalRequestCount=" + approvalRequestCount +
                    '}';
        }
    }
}