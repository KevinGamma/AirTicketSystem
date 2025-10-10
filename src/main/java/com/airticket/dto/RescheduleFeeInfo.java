package com.airticket.dto;

import java.math.BigDecimal;
import java.time.Instant;





public class RescheduleFeeInfo {
    private Long ticketId;
    private String ticketNumber;
    private BigDecimal currentTicketPrice;
    private BigDecimal newTicketPrice;
    
    
    private BigDecimal serviceFee;
    private BigDecimal baseFee;
    private BigDecimal percentageFee;
    private String feeType; 
    
    
    private BigDecimal priceDifference; 
    private BigDecimal totalAdditionalCost; 
    private BigDecimal totalRefund; 
    
    
    private long hoursUntilDeparture;
    private int highFeeThresholdHours;
    private Instant departureTime;
    
    
    private String feeExplanation;
    private String[] feeBreakdown;
    private String recommendation;
    
    
    private FlightInfo originalFlight;
    private FlightInfo newFlight;
    
    public static class FlightInfo {
        private Long flightId;
        private String flightNumber;
        private String departureAirport;
        private String arrivalAirport;
        private Instant departureTime;
        private Instant arrivalTime;
        private BigDecimal price;
        
        public FlightInfo() {}
        
        public FlightInfo(Long flightId, String flightNumber, String departureAirport, 
                         String arrivalAirport, Instant departureTime, Instant arrivalTime, BigDecimal price) {
            this.flightId = flightId;
            this.flightNumber = flightNumber;
            this.departureAirport = departureAirport;
            this.arrivalAirport = arrivalAirport;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.price = price;
        }
        
        
        public Long getFlightId() { return flightId; }
        public void setFlightId(Long flightId) { this.flightId = flightId; }
        
        public String getFlightNumber() { return flightNumber; }
        public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
        
        public String getDepartureAirport() { return departureAirport; }
        public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }
        
        public String getArrivalAirport() { return arrivalAirport; }
        public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }
        
        public Instant getDepartureTime() { return departureTime; }
        public void setDepartureTime(Instant departureTime) { this.departureTime = departureTime; }
        
        public Instant getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(Instant arrivalTime) { this.arrivalTime = arrivalTime; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
    
    public RescheduleFeeInfo() {}
    
    
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    
    public BigDecimal getCurrentTicketPrice() { return currentTicketPrice; }
    public void setCurrentTicketPrice(BigDecimal currentTicketPrice) { this.currentTicketPrice = currentTicketPrice; }
    
    public BigDecimal getNewTicketPrice() { return newTicketPrice; }
    public void setNewTicketPrice(BigDecimal newTicketPrice) { this.newTicketPrice = newTicketPrice; }
    
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    
    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }
    
    public BigDecimal getPercentageFee() { return percentageFee; }
    public void setPercentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; }
    
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    
    public BigDecimal getPriceDifference() { return priceDifference; }
    public void setPriceDifference(BigDecimal priceDifference) { this.priceDifference = priceDifference; }
    
    public BigDecimal getTotalAdditionalCost() { return totalAdditionalCost; }
    public void setTotalAdditionalCost(BigDecimal totalAdditionalCost) { this.totalAdditionalCost = totalAdditionalCost; }
    
    public BigDecimal getTotalRefund() { return totalRefund; }
    public void setTotalRefund(BigDecimal totalRefund) { this.totalRefund = totalRefund; }
    
    public long getHoursUntilDeparture() { return hoursUntilDeparture; }
    public void setHoursUntilDeparture(long hoursUntilDeparture) { this.hoursUntilDeparture = hoursUntilDeparture; }
    
    public int getHighFeeThresholdHours() { return highFeeThresholdHours; }
    public void setHighFeeThresholdHours(int highFeeThresholdHours) { this.highFeeThresholdHours = highFeeThresholdHours; }
    
    public Instant getDepartureTime() { return departureTime; }
    public void setDepartureTime(Instant departureTime) { this.departureTime = departureTime; }
    
    public String getFeeExplanation() { return feeExplanation; }
    public void setFeeExplanation(String feeExplanation) { this.feeExplanation = feeExplanation; }
    
    public String[] getFeeBreakdown() { return feeBreakdown; }
    public void setFeeBreakdown(String[] feeBreakdown) { this.feeBreakdown = feeBreakdown; }
    
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    
    public FlightInfo getOriginalFlight() { return originalFlight; }
    public void setOriginalFlight(FlightInfo originalFlight) { this.originalFlight = originalFlight; }
    
    public FlightInfo getNewFlight() { return newFlight; }
    public void setNewFlight(FlightInfo newFlight) { this.newFlight = newFlight; }
}