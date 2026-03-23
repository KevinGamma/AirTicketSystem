package com.airticket.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class FlightSearchToolResult {

    private boolean searchExecuted;
    private boolean hasResults;
    private String departureDate;
    private String message;
    private LocationResolveResult departure;
    private LocationResolveResult arrival;
    private List<FlightOptionView> flights = new ArrayList<>();
    private List<FlightItineraryView> itineraries = new ArrayList<>();

    public boolean isSearchExecuted() {
        return searchExecuted;
    }

    public void setSearchExecuted(boolean searchExecuted) {
        this.searchExecuted = searchExecuted;
    }

    public boolean isHasResults() {
        return hasResults;
    }

    public void setHasResults(boolean hasResults) {
        this.hasResults = hasResults;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocationResolveResult getDeparture() {
        return departure;
    }

    public void setDeparture(LocationResolveResult departure) {
        this.departure = departure;
    }

    public LocationResolveResult getArrival() {
        return arrival;
    }

    public void setArrival(LocationResolveResult arrival) {
        this.arrival = arrival;
    }

    public List<FlightOptionView> getFlights() {
        return flights;
    }

    public void setFlights(List<FlightOptionView> flights) {
        this.flights = flights;
    }

    public List<FlightItineraryView> getItineraries() {
        return itineraries;
    }

    public void setItineraries(List<FlightItineraryView> itineraries) {
        this.itineraries = itineraries;
    }
}
