package com.airticket.ai.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FlightItineraryView {

    private String type;
    private int segmentCount;
    private List<Long> segmentIds = new ArrayList<>();
    private List<FlightOptionView> segments = new ArrayList<>();
    private String flightNumberSummary;
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
    private BigDecimal totalPrice;
    private Long totalDurationMinutes;
    private Integer availableSeats;
    private String connectionInfo;
    private String transferNotice;
    private boolean requiresAirportSwitch;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }

    public List<Long> getSegmentIds() {
        return segmentIds;
    }

    public void setSegmentIds(List<Long> segmentIds) {
        this.segmentIds = segmentIds;
    }

    public List<FlightOptionView> getSegments() {
        return segments;
    }

    public void setSegments(List<FlightOptionView> segments) {
        this.segments = segments;
    }

    public String getFlightNumberSummary() {
        return flightNumberSummary;
    }

    public void setFlightNumberSummary(String flightNumberSummary) {
        this.flightNumberSummary = flightNumberSummary;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(Long totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(String connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public String getTransferNotice() {
        return transferNotice;
    }

    public void setTransferNotice(String transferNotice) {
        this.transferNotice = transferNotice;
    }

    public boolean isRequiresAirportSwitch() {
        return requiresAirportSwitch;
    }

    public void setRequiresAirportSwitch(boolean requiresAirportSwitch) {
        this.requiresAirportSwitch = requiresAirportSwitch;
    }
}
