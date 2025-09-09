package com.airticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ConnectingFlightSearchRequest extends FlightSearchRequest {
    private boolean includeConnectingFlights = false;
    
    public ConnectingFlightSearchRequest() {
        super();
    }

    public ConnectingFlightSearchRequest(String departureCity, String arrivalCity, LocalDate departureDate, boolean includeConnectingFlights) {
        super(departureCity, arrivalCity, departureDate);
        this.includeConnectingFlights = includeConnectingFlights;
    }

    public boolean isIncludeConnectingFlights() {
        return includeConnectingFlights;
    }

    public void setIncludeConnectingFlights(boolean includeConnectingFlights) {
        this.includeConnectingFlights = includeConnectingFlights;
    }
}