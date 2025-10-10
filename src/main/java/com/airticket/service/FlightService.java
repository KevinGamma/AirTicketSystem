package com.airticket.service;

import com.airticket.dto.FlightSearchRequest;
import com.airticket.dto.ConnectingFlightSearchRequest;
import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.model.Flight;
import com.airticket.model.ConnectingFlight;
import com.airticket.model.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;

import com.airticket.util.TimeZoneUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class FlightService {
    
    @Autowired
    private FlightMapper flightMapper;
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private AirlineService airlineService;
    
    public List<Flight> getAllFlights() {
        return flightMapper.findAllWithAssociations();
    }
    
    public Flight getFlightById(Long id) {
        Flight flight = flightMapper.findById(id);
        if (flight != null) {
            updateFlightStatusBasedOnTime(flight);
        }
        return flight;
    }
    
    public List<Flight> searchFlights(FlightSearchRequest request) {
        System.out.println("=== FLIGHT SEARCH DEBUG ===");
        System.out.println("Search Parameters:");
        System.out.println("  Departure City: '" + request.getDepartureCity() + "'");
        System.out.println("  Arrival City: '" + request.getArrivalCity() + "'");
        System.out.println("  Departure Date: " + request.getDepartureDate());

        System.out.println("\n=== AIRPORT DEBUG INFO ===");
        List<Flight> allFlights = flightMapper.findAllWithAssociations();
        System.out.println("Total flights in database: " + allFlights.size());

        System.out.println("\nAll departure airports for city '" + request.getDepartureCity() + "':");
        allFlights.stream()
            .filter(f -> f.getDepartureAirport() != null && f.getDepartureAirport().getCity() != null)
            .filter(f -> f.getDepartureAirport().getCity().trim().equalsIgnoreCase(request.getDepartureCity().trim()))
            .map(f -> f.getDepartureAirport())
            .distinct()
            .forEach(airport -> System.out.println("  - " + airport.getCode() + " (" + airport.getName() + ") in " + airport.getCity()));
            
        System.out.println("\nAll arrival airports for city '" + request.getArrivalCity() + "':");
        allFlights.stream()
            .filter(f -> f.getArrivalAirport() != null && f.getArrivalAirport().getCity() != null)
            .filter(f -> f.getArrivalAirport().getCity().trim().equalsIgnoreCase(request.getArrivalCity().trim()))
            .map(f -> f.getArrivalAirport())
            .distinct()
            .forEach(airport -> System.out.println("  - " + airport.getCode() + " (" + airport.getName() + ") in " + airport.getCity()));

        System.out.println("\nAll flights departing from '" + request.getDepartureCity() + "' (any date):");
        allFlights.stream()
            .filter(f -> f.getDepartureAirport() != null && f.getDepartureAirport().getCity() != null)
            .filter(f -> f.getDepartureAirport().getCity().trim().equalsIgnoreCase(request.getDepartureCity().trim()))
            .limit(10)
            .forEach(f -> System.out.println("  - " + f.getFlightNumber() + 
                " | " + f.getDepartureAirport().getCode() + "(" + f.getDepartureAirport().getName() + ")" +
                " -> " + f.getArrivalAirport().getCode() + "(" + f.getArrivalAirport().getName() + ")" +
                " | " + f.getDepartureAirport().getCity() + " -> " + f.getArrivalAirport().getCity() +
                " | Date: " + f.getDepartureTimeUtc()));
        
        System.out.println("=== END AIRPORT DEBUG ===\n");
        
        System.out.println("=== EXECUTING SQL SEARCH ===");
        System.out.println("SQL parameters: departureCity='" + request.getDepartureCity() + 
                          "', arrivalCity='" + request.getArrivalCity() + 
                          "', departureDate=" + request.getDepartureDate());
        
        List<Flight> flights = flightMapper.searchFlights(
            request.getDepartureCity(),
            request.getArrivalCity(),
            request.getDepartureDate()
        );
        
        System.out.println("Raw flights returned from database query: " + flights.size());
        if (flights.size() > 0) {
            System.out.println("All flights found by search query:");
            for (int i = 0; i < flights.size(); i++) {
                Flight f = flights.get(i);
                System.out.println("  " + (i+1) + ". " + f.getFlightNumber() + 
                    " | " + f.getDepartureAirport().getCode() + "(" + f.getDepartureAirport().getName() + ")" +
                    " -> " + f.getArrivalAirport().getCode() + "(" + f.getArrivalAirport().getName() + ")" +
                    " | " + f.getDepartureAirport().getCity() + " -> " + f.getArrivalAirport().getCity() +
                    " | Status: " + f.getStatus() + " | Departure: " + f.getDepartureTimeUtc());
            }
        } else {
            System.out.println("NO FLIGHTS FOUND - This indicates the SQL query is not matching any records!");
            System.out.println("Expected query parameters:");
            System.out.println("  - Departure city match: '" + request.getDepartureCity() + "'");  
            System.out.println("  - Arrival city match: '" + request.getArrivalCity() + "'");
            System.out.println("  - Departure date match: " + request.getDepartureDate());
            
            
            System.out.println("\n=== DEBUGGING: Check if flights exist without date filter ===");
            List<Flight> allRouteFlights = allFlights.stream()
                .filter(f -> f.getDepartureAirport() != null && f.getDepartureAirport().getCity() != null)
                .filter(f -> f.getArrivalAirport() != null && f.getArrivalAirport().getCity() != null)
                .filter(f -> f.getDepartureAirport().getCity().trim().equalsIgnoreCase(request.getDepartureCity().trim()))
                .filter(f -> f.getArrivalAirport().getCity().trim().equalsIgnoreCase(request.getArrivalCity().trim()))
                .toList();
            System.out.println("Flights matching route (without date): " + allRouteFlights.size());
            
            if (allRouteFlights.size() > 0) {
                System.out.println("Sample flight dates for this route:");
                allRouteFlights.stream().limit(5).forEach(f -> {
                    System.out.println("  - " + f.getFlightNumber() + " departs: " + f.getDepartureTimeUtc() + 
                                      " (Date part: " + f.getDepartureTimeUtc().toString().substring(0, 10) + ")");
                });
                System.out.println("Search date: " + request.getDepartureDate());
                System.out.println("Possible issue: Date format mismatch between search parameter and stored dates");
            } else {
                System.out.println("No flights found for this route at all - check city name matching");
            }
        }
        
        List<Flight> filteredFlights = flights.stream()
            .filter(flight -> {
                updateFlightStatusBasedOnTime(flight);
                boolean isValid = !flight.getStatus().equals("已起飞") && !flight.getStatus().equals("已降落");
                if (!isValid) {
                    System.out.println("Filtered out flight " + flight.getFlightNumber() + 
                        " due to status: " + flight.getStatus());
                }
                return isValid;
            })
            .toList();
            
        System.out.println("Final flights after status filtering: " + filteredFlights.size());
        if (filteredFlights.size() != flights.size()) {
            System.out.println("Status filtering removed " + (flights.size() - filteredFlights.size()) + " flights");
        }
        System.out.println("=== END FLIGHT SEARCH DEBUG ===");
        
        return filteredFlights;
    }
    
    public List<ConnectingFlight> searchConnectingFlights(ConnectingFlightSearchRequest request) {
        List<ConnectingFlight> result = new ArrayList<>();

        List<Flight> directFlights = searchFlights(request);

        for (Flight flight : directFlights) {
            result.add(new ConnectingFlight(List.of(flight)));
        }

        if (request.isIncludeConnectingFlights()) {
            List<ConnectingFlight> connectingFlights = findConnectingFlights(
                request.getDepartureCity(),
                request.getArrivalCity(),
                request.getDepartureDate()
            );
            result.addAll(connectingFlights);
        }

        return result.stream()
            .sorted(Comparator.comparing(ConnectingFlight::getTotalDurationMinutes))
            .collect(Collectors.toList());
    }

    public List<ConnectingFlight> findConnectingFlights(String departureCity, String arrivalCity, LocalDate departureDate) {
        List<ConnectingFlight> result = new ArrayList<>();

        if (departureCity == null || arrivalCity == null || departureDate == null) {
            System.out.println("参数缺失：departureCity=" + departureCity + ", arrivalCity=" + arrivalCity + ", departureDate=" + departureDate);
            return result;
        }

        
        List<Flight> firstLegFlights = flightMapper.findFlightsFromCity(departureCity, departureDate);
        if (firstLegFlights == null || firstLegFlights.isEmpty()) {
            System.out.println("第一段航班为空: " + departureCity + " 出发, 日期: " + departureDate);
            return result;
        }

        
        Instant now = Instant.now();
        firstLegFlights = firstLegFlights.stream()
            .filter(flight -> {
                if (flight.getDepartureTimeUtc() == null) {
                    return false;
                }
                
                boolean hasNotDeparted = now.isBefore(flight.getDepartureTimeUtc());
                if (!hasNotDeparted) {
                    System.out.println("过滤掉已起飞的第一段航班: " + flight.getFlightNumber() + 
                        ", 起飞时间: " + flight.getDepartureTimeUtc() + ", 当前时间: " + now);
                }
                return hasNotDeparted;
            })
            .collect(Collectors.toList());

        System.out.println("过滤后的第一段航班数量: " + firstLegFlights.size());

        for (Flight firstLeg : firstLegFlights) {
            if (firstLeg.getArrivalTimeUtc() == null) {
                System.out.println("跳过第一段航班 (到达时间为空): " + firstLeg);
                continue;
            }

            Instant firstLegArrival = firstLeg.getArrivalTimeUtc();
            Instant earliestSecondLegDeparture = firstLegArrival.plusSeconds(3600); 

            
            String intermediateCity = firstLeg.getArrivalAirport() != null ? firstLeg.getArrivalAirport().getCity() : null;
            List<Flight> secondLegFlights = new ArrayList<>();
            
            if (intermediateCity != null) {
                
                secondLegFlights.addAll(flightMapper.findConnectingFlightsFromCityToCity(
                    intermediateCity, arrivalCity, departureDate, earliestSecondLegDeparture));
                secondLegFlights.addAll(flightMapper.findConnectingFlightsFromCityToCity(
                    intermediateCity, arrivalCity, departureDate.plusDays(1), earliestSecondLegDeparture));
            } else {
                
                secondLegFlights.addAll(flightMapper.findFlightsToCity(arrivalCity, departureDate, earliestSecondLegDeparture));
                secondLegFlights.addAll(flightMapper.findFlightsToCity(arrivalCity, departureDate.plusDays(1), earliestSecondLegDeparture));
            }

            System.out.println("第一段航班: " + firstLeg.getFlightNumber() + " 到达时间: " + firstLegArrival +
                    ", 可用第二段航班数量: " + secondLegFlights.size());

            for (Flight secondLeg : secondLegFlights) {
                if (secondLeg.getDepartureTimeUtc() == null) {
                    System.out.println("跳过第二段航班 (起飞时间为空): " + secondLeg);
                    continue;
                }

                
                boolean canConnect = false;
                boolean sameCityDifferentAirport = false;
                
                if (firstLeg.getArrivalAirportId().equals(secondLeg.getDepartureAirportId())) {
                    
                    canConnect = true;
                } else if (firstLeg.getArrivalAirport() != null && 
                          secondLeg.getDepartureAirport() != null &&
                          firstLeg.getArrivalAirport().getCity() != null && 
                          secondLeg.getDepartureAirport().getCity() != null) {
                    
                    String firstArrivalCity = firstLeg.getArrivalAirport().getCity().trim();
                    String secondDepartureCity = secondLeg.getDepartureAirport().getCity().trim();
                    if (firstArrivalCity.equalsIgnoreCase(secondDepartureCity)) {
                        canConnect = true;
                        sameCityDifferentAirport = true;
                    }
                }
                
                if (!canConnect) {
                    System.out.println("组合被过滤 (机场/城市不匹配): " + firstLeg.getFlightNumber() + 
                            " 到达机场: " + (firstLeg.getArrivalAirport() != null ? firstLeg.getArrivalAirport().getCode() + "(" + firstLeg.getArrivalAirport().getCity() + ")" : "N/A") +
                            " != " + secondLeg.getFlightNumber() + " 出发机场: " + (secondLeg.getDepartureAirport() != null ? secondLeg.getDepartureAirport().getCode() + "(" + secondLeg.getDepartureAirport().getCity() + ")" : "N/A"));
                    continue;
                }

                
                long requiredConnectionMinutes = sameCityDifferentAirport ? 120 : 60; 
                Instant minimumSecondLegDeparture = firstLegArrival.plusSeconds(requiredConnectionMinutes * 60);
                
                if (!secondLeg.getDepartureTimeUtc().isBefore(minimumSecondLegDeparture)) {
                    List<Flight> flights = List.of(firstLeg, secondLeg);
                    ConnectingFlight connectingFlight = new ConnectingFlight(flights);
                    
                    
                    if (sameCityDifferentAirport) {
                        connectingFlight.setType("CONNECTING_INTERCITY_TRANSFER");
                    }
                    
                    result.add(connectingFlight);
                    String connectionType = sameCityDifferentAirport ? " (同城不同机场)" : " (同机场)";
                    System.out.println("组合成功: " + firstLeg.getFlightNumber() + " -> " +
                            secondLeg.getFlightNumber() + connectionType + ", 衔接时间 (分钟): " +
                            Duration.between(firstLegArrival, secondLeg.getDepartureTimeUtc()).toMinutes());
                    
                    
                    if (connectingFlight.getTransferNotice() != null && !connectingFlight.getTransferNotice().isEmpty()) {
                        System.out.println("转机提醒: " + connectingFlight.getTransferNotice());
                    }
                } else {
                    String minTimeDesc = sameCityDifferentAirport ? "2小时 (同城不同机场)" : "1小时 (同机场)";
                    System.out.println("组合被过滤 (衔接时间不足" + minTimeDesc + "): " + firstLeg.getFlightNumber() + " -> " +
                            secondLeg.getFlightNumber() + ", 衔接时间 (分钟): " +
                            Duration.between(firstLegArrival, secondLeg.getDepartureTimeUtc()).toMinutes());
                }
            }
        }

        System.out.println("总共找到中转航班数量: " + result.size());
        return result;
    }


    public Flight createFlight(Flight flight) {
        
        validateFlightNumber(flight);
        
        
        if (flightMapper.countByFlightNumber(flight.getFlightNumber()) > 0) {
            throw new RuntimeException(messageService.getMessage("flight.duplicate.flightNumber", flight.getFlightNumber()));
        }
        
        flightMapper.insert(flight);
        return flight;
    }
    
    public Flight updateFlight(Flight flight) {
        
        Flight existingFlight = flightMapper.findById(flight.getId());
        if (existingFlight == null) {
            throw new RuntimeException("Flight not found with id: " + flight.getId());
        }
        
        
        validateFlightNumber(flight);
        
        
        if (flightMapper.countByFlightNumberExcludeId(flight.getFlightNumber(), flight.getId()) > 0) {
            throw new RuntimeException(messageService.getMessage("flight.duplicate.flightNumber", flight.getFlightNumber()));
        }
        
        
        if (flight.getAvailableSeats() == null) {
            flight.setAvailableSeats(existingFlight.getAvailableSeats());
        }
        
        
        if (flight.getStatus() == null || flight.getStatus().isEmpty()) {
            flight.setStatus(existingFlight.getStatus());
        }
        
        flightMapper.update(flight);
        return flightMapper.findById(flight.getId());
    }
    
    public void deleteFlight(Long id) {
        
        int ticketCount = ticketMapper.countByFlightId(id);
        if (ticketCount > 0) {
            throw new RuntimeException(messageService.getMessage("flight.delete.hasTickets", ticketCount));
        }
        
        flightMapper.deleteById(id);
    }
    
    public boolean reserveSeat(Long flightId) {
        Flight flight = flightMapper.findById(flightId);
        if (flight == null || flight.getAvailableSeats() <= 0) {
            return false;
        }
        
        
        updateFlightStatusBasedOnTime(flight);
        if (flight.getStatus().equals("已起飞") || flight.getStatus().equals("已降落")) {
            throw new RuntimeException(messageService.getMessage("flight.booking.departedFlight"));
        }
        
        flightMapper.decreaseAvailableSeats(flightId, 1);
        return true;
    }
    
    public void releaseSeat(Long flightId) {
        flightMapper.increaseAvailableSeats(flightId, 1);
    }
    
    public void cancelFlight(Long flightId, String reason) {
        Flight flight = flightMapper.findById(flightId);
        if (flight != null) {
            flight.setStatus("CANCELLED");
            flightMapper.update(flight);
        }
    }
    
    



    private void updateFlightStatusBasedOnTime(Flight flight) {
        if (flight == null) return;
        
        Instant now = Instant.now();
        String currentStatus = flight.getStatus();
        
        
        if ("CANCELLED".equals(currentStatus) || "DELAYED".equals(currentStatus)) {
            return;
        }
        
        String newStatus = null;
        
        if (flight.getArrivalTimeUtc() != null && now.isAfter(flight.getArrivalTimeUtc())) {
            
            newStatus = "已降落";
        } else if (flight.getDepartureTimeUtc() != null && now.isAfter(flight.getDepartureTimeUtc())) {
            
            newStatus = "已起飞";
        } else {
            
            if (!"SCHEDULED".equals(currentStatus) && !"DELAYED".equals(currentStatus)) {
                newStatus = "SCHEDULED";
            }
        }
        
        
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            flight.setStatus(newStatus);
            flightMapper.updateStatus(flight.getId(), newStatus);
        }
    }
    
    


    public void updateAllFlightStatuses() {
        List<Flight> flights = flightMapper.findAll();
        for (Flight flight : flights) {
            updateFlightStatusBasedOnTime(flight);
        }
    }
    
    


    private void validateFlightNumber(Flight flight) {
        if (flight.getAirlineId() == null || flight.getFlightNumber() == null) {
            return;
        }
        
        var airline = airlineService.getAirlineById(flight.getAirlineId());
        if (airline == null) {
            throw new RuntimeException("Airline not found");
        }
        
        if (!airlineService.validateFlightNumber(flight.getFlightNumber(), airline.getCode())) {
            throw new RuntimeException(
                messageService.getMessage("flight.invalidFlightNumber", airline.getCode())
            );
        }
    }
}