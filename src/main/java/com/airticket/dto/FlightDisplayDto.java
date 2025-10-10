package com.airticket.dto;

import com.airticket.model.Flight;
import com.airticket.model.Airline;
import com.airticket.model.Airport;
import com.airticket.util.TimeZoneUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class FlightDisplayDto {
    private Long id;
    private String flightNumber;
    private String airlineName;
    
    private String departureAirportCode;
    private String departureAirportName;
    private String departureCity;
    private String departureCountry;
    private String departureTime;
    private String departureTimeZone;
    private String departureTimeUtc;
    
    private String arrivalAirportCode;
    private String arrivalAirportName;
    private String arrivalCity;
    private String arrivalCountry;
    private String arrivalTime;
    private String arrivalTimeUtc;
    private String arrivalTimeZone;
    
    private String duration;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal price;
    private String status;
    private String aircraftType;

    private Airline airline;
    private Airport departureAirport;
    private Airport arrivalAirport;
    
    public static FlightDisplayDto fromFlight(Flight flight) {
        FlightDisplayDto dto = new FlightDisplayDto();
        dto.id = flight.getId();
        dto.flightNumber = flight.getFlightNumber();
        dto.totalSeats = flight.getTotalSeats();
        dto.availableSeats = flight.getAvailableSeats();
        dto.price = flight.getPrice();
        dto.status = flight.getStatus();
        dto.aircraftType = flight.getAircraftType();

        if (flight.getAirline() != null) {
            dto.airlineName = flight.getAirline().getName();
            dto.airline = flight.getAirline();
        }
        if (flight.getDepartureAirport() != null) {
            dto.departureAirportCode = flight.getDepartureAirport().getCode();
            dto.departureAirportName = flight.getDepartureAirport().getName();
            dto.departureCity = flight.getDepartureAirport().getCity();
            dto.departureAirport = flight.getDepartureAirport();
        }
        
        if (flight.getArrivalAirport() != null) {
            dto.arrivalAirportCode = flight.getArrivalAirport().getCode();
            dto.arrivalAirportName = flight.getArrivalAirport().getName();
            dto.arrivalCity = flight.getArrivalAirport().getCity();
            dto.arrivalAirport = flight.getArrivalAirport();
        }

        dto.departureCountry = getCountryFromCity(dto.departureCity);
        dto.departureTimeZone = getTimezoneFromCity(dto.departureCity);
        dto.arrivalCountry = getCountryFromCity(dto.arrivalCity);
        dto.arrivalTimeZone = getTimezoneFromCity(dto.arrivalCity);

        boolean isInternational = isInternationalFlight(dto);

        if (flight.getDepartureTimeUtc() != null) {
            dto.departureTimeUtc = flight.getDepartureTimeUtc().toString();
            
            ZoneId departureZone;
            if (isInternational && dto.departureTimeZone != null && !dto.departureTimeZone.trim().isEmpty()) {
                departureZone = TimeZoneUtil.parseTimeZone(dto.departureTimeZone);
            } else {
                departureZone = ZoneId.of("Asia/Shanghai");
            }
            
            
            if (!isInternational) {
                
                dto.departureTime = flight.getDepartureTimeUtc().atZone(departureZone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } else {
                
                dto.departureTime = flight.getDepartureTimeUtc().atZone(departureZone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
        }

        if (flight.getArrivalTimeUtc() != null) {
            dto.arrivalTimeUtc = flight.getArrivalTimeUtc().toString();
            
            ZoneId arrivalZone;
            if (isInternational && dto.arrivalTimeZone != null && !dto.arrivalTimeZone.trim().isEmpty()) {
                arrivalZone = TimeZoneUtil.parseTimeZone(dto.arrivalTimeZone);
            } else {
                arrivalZone = ZoneId.of("Asia/Shanghai");
            }
            
            
            if (!isInternational) {
                
                dto.arrivalTime = flight.getArrivalTimeUtc().atZone(arrivalZone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } else {
                
                dto.arrivalTime = flight.getArrivalTimeUtc().atZone(arrivalZone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
        }
        
        if (flight.getDepartureTimeUtc() != null && flight.getArrivalTimeUtc() != null) {
            Duration flightDuration = TimeZoneUtil.calculateFlightDuration(
                flight.getDepartureTimeUtc(), flight.getArrivalTimeUtc());
            dto.duration = formatDuration(flightDuration);
        }
        
        return dto;
    }
    
    private static String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "0h 0m";
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }
    

    private static String getCountryFromCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "中国"; 
        }

        city = city.trim().toUpperCase();
        switch (city) {
            case "LOS ANGELES":
            case "洛杉矶":
            case "NEW YORK":
            case "纽约":
            case "SAN FRANCISCO":
            case "旧金山":
                return "美国";
            case "TOKYO":
            case "东京":
                return "日本";
            case "LONDON":
            case "伦敦":
                return "英国";
            case "PARIS":
            case "巴黎":
                return "法国";
            case "SEOUL":
            case "首尔":
                return "韩国";
            case "SINGAPORE":
            case "新加坡":
                return "新加坡";
            case "BANGKOK":
            case "曼谷":
                return "泰国";
            case "SYDNEY":
            case "悉尼":
                return "澳大利亚";
            default:
                return "中国";
        }
    }

    private static String getTimezoneFromCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "Asia/Shanghai";
        }

        city = city.trim().toUpperCase();
        switch (city) {
            case "LOS ANGELES":
            case "洛杉矶":
            case "SAN FRANCISCO":
            case "旧金山":
                return "America/Los_Angeles";
            case "NEW YORK":
            case "纽约":
                return "America/New_York";
            case "TOKYO":
            case "东京":
                return "Asia/Tokyo";
            case "LONDON":
            case "伦敦":
                return "Europe/London";
            case "PARIS":
            case "巴黎":
                return "Europe/Paris";
            case "SEOUL":
            case "首尔":
                return "Asia/Seoul";
            case "SINGAPORE":
            case "新加坡":
                return "Asia/Singapore";
            case "BANGKOK":
            case "曼谷":
                return "Asia/Bangkok";
            case "SYDNEY":
            case "悉尼":
                return "Australia/Sydney";
            default:
                return "Asia/Shanghai";
        }
    }

    private static boolean isInternationalFlight(FlightDisplayDto dto) {
        if (dto.departureCountry == null || dto.arrivalCountry == null) {
            return false;
        }
        
        try {

            String chinaCountry = "中国";
            String chinaCountryEn = "China";
            
            String depCountry = dto.departureCountry.trim();
            String arrCountry = dto.arrivalCountry.trim();
            
            boolean isDepartureChina = chinaCountry.equals(depCountry) || chinaCountryEn.equalsIgnoreCase(depCountry);
            boolean isArrivalChina = chinaCountry.equals(arrCountry) || chinaCountryEn.equalsIgnoreCase(arrCountry);

            return !(isDepartureChina && isArrivalChina);
        } catch (Exception e) {
            return false;
        }
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    
    public String getDepartureAirportCode() { return departureAirportCode; }
    public void setDepartureAirportCode(String departureAirportCode) { this.departureAirportCode = departureAirportCode; }
    
    public String getDepartureAirportName() { return departureAirportName; }
    public void setDepartureAirportName(String departureAirportName) { this.departureAirportName = departureAirportName; }
    
    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    
    public String getDepartureCountry() { return departureCountry; }
    public void setDepartureCountry(String departureCountry) { this.departureCountry = departureCountry; }
    
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    
    public String getDepartureTimeUtc() { return departureTimeUtc; }
    public void setDepartureTimeUtc(String departureTimeUtc) { this.departureTimeUtc = departureTimeUtc; }
    
    public String getDepartureTimeZone() { return departureTimeZone; }
    public void setDepartureTimeZone(String departureTimeZone) { this.departureTimeZone = departureTimeZone; }
    
    public String getArrivalAirportCode() { return arrivalAirportCode; }
    public void setArrivalAirportCode(String arrivalAirportCode) { this.arrivalAirportCode = arrivalAirportCode; }
    
    public String getArrivalAirportName() { return arrivalAirportName; }
    public void setArrivalAirportName(String arrivalAirportName) { this.arrivalAirportName = arrivalAirportName; }
    
    public String getArrivalCity() { return arrivalCity; }
    public void setArrivalCity(String arrivalCity) { this.arrivalCity = arrivalCity; }
    
    public String getArrivalCountry() { return arrivalCountry; }
    public void setArrivalCountry(String arrivalCountry) { this.arrivalCountry = arrivalCountry; }
    
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    
    public String getArrivalTimeUtc() { return arrivalTimeUtc; }
    public void setArrivalTimeUtc(String arrivalTimeUtc) { this.arrivalTimeUtc = arrivalTimeUtc; }
    
    public String getArrivalTimeZone() { return arrivalTimeZone; }
    public void setArrivalTimeZone(String arrivalTimeZone) { this.arrivalTimeZone = arrivalTimeZone; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }
    
    public Airline getAirline() { return airline; }
    public void setAirline(Airline airline) { this.airline = airline; }
    
    public Airport getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(Airport departureAirport) { this.departureAirport = departureAirport; }
    
    public Airport getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(Airport arrivalAirport) { this.arrivalAirport = arrivalAirport; }
    
}