package com.airticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class Notification {
    private Long id;
    private Long userId;
    private Long ticketId;
    private Long flightId;
    private Long requestId;
    private String notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private Instant scheduledTime;
    private Instant sentTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Instant createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Instant updatedAt;

    
    private Ticket ticket;
    private Flight flight;
    private User user;

    
    public Notification() {}

    public Notification(Long userId, String notificationType, String title, String message) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    
    public static final String TYPE_FLIGHT_REMINDER = "FLIGHT_REMINDER";
    public static final String TYPE_FLIGHT_TAKEOFF = "FLIGHT_TAKEOFF";
    public static final String TYPE_FLIGHT_LANDING = "FLIGHT_LANDING";
    public static final String TYPE_RESCHEDULE_APPROVED = "RESCHEDULE_APPROVED";
    public static final String TYPE_RESCHEDULE_REJECTED = "RESCHEDULE_REJECTED";
    public static final String TYPE_REFUND_PROCESSED = "REFUND_PROCESSED";
    public static final String TYPE_PAYMENT_REQUIRED = "PAYMENT_REQUIRED";
    public static final String TYPE_REFUND_DIFFERENCE = "REFUND_DIFFERENCE";

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Instant getSentTime() {
        return sentTime;
    }

    public void setSentTime(Instant sentTime) {
        this.sentTime = sentTime;
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

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
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
}