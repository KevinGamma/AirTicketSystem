package com.airticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;

public class ConnectingFlight {
    private List<Flight> flights;
    private BigDecimal totalPrice;
    private long totalDurationMinutes;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant departureTimeUtc;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant arrivalTimeUtc;
    private int availableSeats;
    private String type; 
    private String connectionInfo; 
    private String transferNotice; 
    private boolean requiresAirportSwitch; 

    public ConnectingFlight() {}

    public ConnectingFlight(List<Flight> flights) {
        this.flights = flights;
        calculateMetrics();
    }

    private void calculateMetrics() {
        if (flights == null || flights.isEmpty()) {
            this.totalPrice = BigDecimal.ZERO;
            this.totalDurationMinutes = 0;
            this.availableSeats = 0;
            this.departureTimeUtc = null;
            this.arrivalTimeUtc = null;
            this.type = "DIRECT";
            return;
        }

        this.type = flights.size() == 1 ? "DIRECT" : "CONNECTING";
        
        
        if (flights.size() > 1) {
            StringBuilder info = new StringBuilder();
            StringBuilder notice = new StringBuilder();
            boolean hasAirportSwitch = false;
            
            for (int i = 0; i < flights.size() - 1; i++) {
                Flight currentFlight = flights.get(i);
                Flight nextFlight = flights.get(i + 1);
                if (currentFlight.getArrivalAirport() != null && nextFlight.getDepartureAirport() != null) {
                    String currentArrival = currentFlight.getArrivalAirport().getCode();
                    String currentArrivalName = currentFlight.getArrivalAirport().getName();
                    String nextDeparture = nextFlight.getDepartureAirport().getCode();
                    String nextDepartureName = nextFlight.getDepartureAirport().getName();
                    String cityName = currentFlight.getArrivalAirport().getCity();
                    
                    if (!currentArrival.equals(nextDeparture)) {
                        
                        hasAirportSwitch = true;
                        info.append("中转: ").append(currentArrival).append("→").append(nextDeparture);
                        if (cityName != null) {
                            info.append("(").append(cityName).append("同城不同机场)");
                        }
                        
                        
                        if (notice.length() > 0) notice.append(" ");
                        notice.append("⚠️ 重要提醒：需要在").append(cityName).append("换机场！")
                              .append("从").append(currentArrivalName != null ? currentArrivalName : currentArrival)
                              .append("(").append(currentArrival).append(")")
                              .append("前往").append(nextDepartureName != null ? nextDepartureName : nextDeparture)
                              .append("(").append(nextDeparture).append(")")
                              .append("，请预留充足的地面交通时间。");
                    } else {
                        
                        info.append("中转: ").append(currentArrival).append("(同机场)");
                    }
                    if (i < flights.size() - 2) info.append("; ");
                }
            }
            this.connectionInfo = info.toString();
            this.transferNotice = notice.toString();
            this.requiresAirportSwitch = hasAirportSwitch;
        }

        
        Flight firstFlight = flights.get(0);
        Flight lastFlight = flights.get(flights.size() - 1);
        this.departureTimeUtc = firstFlight.getDepartureTimeUtc();
        this.arrivalTimeUtc = lastFlight.getArrivalTimeUtc();
        if (departureTimeUtc != null && arrivalTimeUtc != null) {
            this.totalDurationMinutes = Duration.between(departureTimeUtc, arrivalTimeUtc).toMinutes();
        } else {
            this.totalDurationMinutes = 0;
        }

        
        this.totalPrice = flights.stream()
                .map(f -> f.getPrice() != null ? f.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        
        this.availableSeats = flights.stream()
                .map(f -> f.getAvailableSeats() != null ? f.getAvailableSeats() : 0)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);

    }

    
    public List<Flight> getFlights() {
        return flights;
    }

    public void setFlights(List<Flight> flights) {
        this.flights = flights;
        calculateMetrics();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public long getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(long totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
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

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
