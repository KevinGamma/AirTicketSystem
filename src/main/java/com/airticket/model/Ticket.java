package com.airticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Instant;
import java.util.List;

public class Ticket {
    private Long id;
    private String ticketNumber;
    private Long userId;
    private Long flightId;
    private String seatNumber;
    private String passengerName;
    private String passengerIdNumber;
    private String ticketType;
    private BigDecimal price;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant bookingTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant paymentTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
    private boolean deletedByUser = false; 
    
    
    private BigDecimal serviceFee;
    private String rescheduleReason;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant rescheduleTime;
    private String refundReason;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant refundTime;
    
    
    private Long originalTicketId; 
    private Long rescheduledToTicketId; 
    private Boolean isOriginalTicket; 
    
    private Flight flight;
    private User user;
    private List<Flight> connectingFlights;
    
    
    private transient Long paymentRemainingSeconds;
    private transient String paymentCountdownDisplay;
    private transient String paymentUrgencyLevel;
    private transient String paymentMessage;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private transient Instant paymentDeadline;
    private transient Boolean canPay;

    public Ticket() {}

    public Ticket(String ticketNumber, Long userId, Long flightId, String passengerName, 
                  String passengerIdNumber, String ticketType, BigDecimal price) {
        this.ticketNumber = ticketNumber;
        this.userId = userId;
        this.flightId = flightId;
        this.passengerName = passengerName;
        this.passengerIdNumber = passengerIdNumber;
        this.ticketType = ticketType;
        this.price = price;
        this.status = "BOOKED";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerIdNumber() {
        return passengerIdNumber;
    }

    public void setPassengerIdNumber(String passengerIdNumber) {
        this.passengerIdNumber = passengerIdNumber;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(Instant bookingTime) {
        this.bookingTime = bookingTime;
    }

    public Instant getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Instant paymentTime) {
        this.paymentTime = paymentTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Flight> getConnectingFlights() {
        return connectingFlights;
    }

    public void setConnectingFlights(List<Flight> connectingFlights) {
        this.connectingFlights = connectingFlights;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public String getRescheduleReason() {
        return rescheduleReason;
    }

    public void setRescheduleReason(String rescheduleReason) {
        this.rescheduleReason = rescheduleReason;
    }

    public Instant getRescheduleTime() {
        return rescheduleTime;
    }

    public void setRescheduleTime(Instant rescheduleTime) {
        this.rescheduleTime = rescheduleTime;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Instant getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(Instant refundTime) {
        this.refundTime = refundTime;
    }

    public boolean isDeletedByUser() {
        return deletedByUser;
    }

    public void setDeletedByUser(boolean deletedByUser) {
        this.deletedByUser = deletedByUser;
    }
    
    
    public Long getPaymentRemainingSeconds() {
        return paymentRemainingSeconds;
    }
    
    public void setPaymentRemainingSeconds(Long paymentRemainingSeconds) {
        this.paymentRemainingSeconds = paymentRemainingSeconds;
    }
    
    public String getPaymentCountdownDisplay() {
        return paymentCountdownDisplay;
    }
    
    public void setPaymentCountdownDisplay(String paymentCountdownDisplay) {
        this.paymentCountdownDisplay = paymentCountdownDisplay;
    }
    
    public String getPaymentUrgencyLevel() {
        return paymentUrgencyLevel;
    }
    
    public void setPaymentUrgencyLevel(String paymentUrgencyLevel) {
        this.paymentUrgencyLevel = paymentUrgencyLevel;
    }
    
    public String getPaymentMessage() {
        return paymentMessage;
    }
    
    public void setPaymentMessage(String paymentMessage) {
        this.paymentMessage = paymentMessage;
    }
    
    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }
    
    public void setPaymentDeadline(Instant paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }
    
    public Boolean getCanPay() {
        return canPay;
    }
    
    public void setCanPay(Boolean canPay) {
        this.canPay = canPay;
    }
    
    
    public Long getOriginalTicketId() {
        return originalTicketId;
    }
    
    public void setOriginalTicketId(Long originalTicketId) {
        this.originalTicketId = originalTicketId;
    }
    
    public Long getRescheduledToTicketId() {
        return rescheduledToTicketId;
    }
    
    public void setRescheduledToTicketId(Long rescheduledToTicketId) {
        this.rescheduledToTicketId = rescheduledToTicketId;
    }
    
    public Boolean getIsOriginalTicket() {
        return isOriginalTicket;
    }
    
    public void setIsOriginalTicket(Boolean isOriginalTicket) {
        this.isOriginalTicket = isOriginalTicket;
    }
}