package com.airticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class FlightSearchRequest {
    @NotBlank(message = "{validation.departureCity.required}")
    private String departureCity;
    
    @NotBlank(message = "{validation.arrivalCity.required}")
    private String arrivalCity;
    
    @NotNull(message = "{validation.departureDate.required}")
    private LocalDate departureDate;
    
    private LocalDate returnDate;
    private Integer passengers = 1;
    private String ticketType = "ECONOMY";

    public FlightSearchRequest() {}

    public FlightSearchRequest(String departureCity, String arrivalCity, LocalDate departureDate) {
        this.departureCity = departureCity;
        this.arrivalCity = arrivalCity;
        this.departureDate = departureDate;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public void setArrivalCity(String arrivalCity) {
        this.arrivalCity = arrivalCity;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public Integer getPassengers() {
        return passengers;
    }

    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }
    
    @Override
    public String toString() {
        return "FlightSearchRequest{" +
                "departureCity='" + departureCity + '\'' +
                ", arrivalCity='" + arrivalCity + '\'' +
                ", departureDate=" + departureDate +
                ", returnDate=" + returnDate +
                ", passengers=" + passengers +
                ", ticketType='" + ticketType + '\'' +
                '}';
    }
}