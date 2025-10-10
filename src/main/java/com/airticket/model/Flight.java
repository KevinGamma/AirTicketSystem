package com.airticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public class Flight {
    private Long id;
    private String flightNumber;
    private Long airlineId;
    private Long departureAirportId;
    private Long arrivalAirportId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant departureTimeUtc;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant arrivalTimeUtc;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal price;
    private String status;
    private String aircraftType;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
    
    private Airport departureAirport;
    private Airport arrivalAirport;
    private Airline airline;
    private java.util.List<Flight> connectingFlights;

    public Flight() {}

    public Flight(String flightNumber, Long airlineId, Long departureAirportId, Long arrivalAirportId, 
                  Instant departureTimeUtc, Instant arrivalTimeUtc, Integer totalSeats, 
                  BigDecimal price) {
        this.flightNumber = flightNumber;
        this.airlineId = airlineId;
        this.departureAirportId = departureAirportId;
        this.arrivalAirportId = arrivalAirportId;
        this.departureTimeUtc = departureTimeUtc;
        this.arrivalTimeUtc = arrivalTimeUtc;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.price = price;
        this.status = "SCHEDULED";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Long getAirlineId() {
        return airlineId;
    }

    public void setAirlineId(Long airlineId) {
        this.airlineId = airlineId;
    }

    public Long getDepartureAirportId() {
        return departureAirportId;
    }

    public void setDepartureAirportId(Long departureAirportId) {
        this.departureAirportId = departureAirportId;
    }

    public Long getArrivalAirportId() {
        return arrivalAirportId;
    }

    public void setArrivalAirportId(Long arrivalAirportId) {
        this.arrivalAirportId = arrivalAirportId;
    }

    public Instant getDepartureTimeUtc() {
        return departureTimeUtc;
    }

    public void setDepartureTimeUtc(Instant departureTimeUtc) {
        this.departureTimeUtc = departureTimeUtc;
    }

    public Instant getArrivalTimeUtc() {
        return arrivalTimeUtc;
    }

    public void setArrivalTimeUtc(Instant arrivalTimeUtc) {
        this.arrivalTimeUtc = arrivalTimeUtc;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
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

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
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

    public Airport getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(Airport departureAirport) {
        this.departureAirport = departureAirport;
    }

    public Airport getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(Airport arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public Airline getAirline() {
        return airline;
    }

    public void setAirline(Airline airline) {
        this.airline = airline;
    }

    @JsonProperty("departureTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getDepartureTime() {
        System.out.println("Flight.getDepartureTime() called for flight " + this.id + 
                          " (" + this.flightNumber + "): returning " + this.departureTimeUtc);
        return this.departureTimeUtc;
    }

    public void setDepartureTime(Instant departureTime) {
        System.out.println("Flight.setDepartureTime() called for flight " + this.id + 
                          " (" + this.flightNumber + "): setting to " + departureTime);
        this.departureTimeUtc = departureTime;
    }

    @JsonProperty("arrivalTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getArrivalTime() {
        System.out.println("Flight.getArrivalTime() called for flight " + this.id + 
                          " (" + this.flightNumber + "): returning " + this.arrivalTimeUtc);
        return this.arrivalTimeUtc;  
    }

    public void setArrivalTime(Instant arrivalTime) {
        System.out.println("Flight.setArrivalTime() called for flight " + this.id + 
                          " (" + this.flightNumber + "): setting to " + arrivalTime);
        this.arrivalTimeUtc = arrivalTime;
    }

    public java.util.List<Flight> getConnectingFlights() {
        return connectingFlights;
    }

    public void setConnectingFlights(java.util.List<Flight> connectingFlights) {
        this.connectingFlights = connectingFlights;
    }

}