package com.airticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookingRequest {
    @NotNull(message = "Flight ID is required")
    private Long flightId;
    
    @NotBlank(message = "Passenger name is required")
    private String passengerName;
    
    @NotBlank(message = "Passenger ID number is required")
    private String passengerIdNumber;
    
    private String ticketType = "ECONOMY";
    private String seatNumber;
    private List<Long> connectingFlightIds;

    public BookingRequest() {
    }

    public BookingRequest(Long flightId, String passengerName, String passengerIdNumber) {
        this.flightId = flightId;
        this.passengerName = passengerName;
        this.passengerIdNumber = passengerIdNumber;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
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

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public List<Long> getConnectingFlightIds() {
        return connectingFlightIds;
    }

    public void setConnectingFlightIds(List<Long> connectingFlightIds) {
        this.connectingFlightIds = connectingFlightIds;
    }
}
