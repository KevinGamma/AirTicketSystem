package com.airticket.ai.dto;

import java.math.BigDecimal;

public class FlightOptionView {

    private Long id;
    private String flightNumber;
    private String airlineName;
    private String departureAirportCode;
    private String departureAirportName;
    private String departureCity;
    private String departureTimeUtc;
    private String departureTimeLocal;
    private String arrivalAirportCode;
    private String arrivalAirportName;
    private String arrivalCity;
    private String arrivalTimeUtc;
    private String arrivalTimeLocal;
    private BigDecimal price;
    private Integer availableSeats;
    private String status;
    private String aircraftType;

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

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getDepartureAirportCode() {
        return departureAirportCode;
    }

    public void setDepartureAirportCode(String departureAirportCode) {
        this.departureAirportCode = departureAirportCode;
    }

    public String getDepartureAirportName() {
        return departureAirportName;
    }

    public void setDepartureAirportName(String departureAirportName) {
        this.departureAirportName = departureAirportName;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getDepartureTimeUtc() {
        return departureTimeUtc;
    }

    public void setDepartureTimeUtc(String departureTimeUtc) {
        this.departureTimeUtc = departureTimeUtc;
    }

    public String getDepartureTimeLocal() {
        return departureTimeLocal;
    }

    public void setDepartureTimeLocal(String departureTimeLocal) {
        this.departureTimeLocal = departureTimeLocal;
    }

    public String getArrivalAirportCode() {
        return arrivalAirportCode;
    }

    public void setArrivalAirportCode(String arrivalAirportCode) {
        this.arrivalAirportCode = arrivalAirportCode;
    }

    public String getArrivalAirportName() {
        return arrivalAirportName;
    }

    public void setArrivalAirportName(String arrivalAirportName) {
        this.arrivalAirportName = arrivalAirportName;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public void setArrivalCity(String arrivalCity) {
        this.arrivalCity = arrivalCity;
    }

    public String getArrivalTimeUtc() {
        return arrivalTimeUtc;
    }

    public void setArrivalTimeUtc(String arrivalTimeUtc) {
        this.arrivalTimeUtc = arrivalTimeUtc;
    }

    public String getArrivalTimeLocal() {
        return arrivalTimeLocal;
    }

    public void setArrivalTimeLocal(String arrivalTimeLocal) {
        this.arrivalTimeLocal = arrivalTimeLocal;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
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
}
