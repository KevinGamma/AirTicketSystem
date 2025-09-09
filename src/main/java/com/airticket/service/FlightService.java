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

        // 查询出所有第一段航班
        List<Flight> firstLegFlights = flightMapper.findFlightsFromCity(departureCity, departureDate);
        if (firstLegFlights == null || firstLegFlights.isEmpty()) {
            System.out.println("第一段航班为空: " + departureCity + " 出发, 日期: " + departureDate);
            return result;
        }

        // 过滤掉已经起飞的第一段航班
        Instant now = Instant.now();
        firstLegFlights = firstLegFlights.stream()
            .filter(flight -> {
                if (flight.getDepartureTimeUtc() == null) {
                    return false;
                }
                // 只保留还没起飞的航班
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
            Instant earliestSecondLegDeparture = firstLegArrival.plusSeconds(3600); // 衔接时间 >= 1 小时

            // 查询第二段航班，从第一段的到达城市出发到最终目的地，日期可能是当天或次日
            String intermediateCity = firstLeg.getArrivalAirport() != null ? firstLeg.getArrivalAirport().getCity() : null;
            List<Flight> secondLegFlights = new ArrayList<>();
            
            if (intermediateCity != null) {
                // 使用新的方法查找从中转城市到目的地的航班
                secondLegFlights.addAll(flightMapper.findConnectingFlightsFromCityToCity(
                    intermediateCity, arrivalCity, departureDate, earliestSecondLegDeparture));
                secondLegFlights.addAll(flightMapper.findConnectingFlightsFromCityToCity(
                    intermediateCity, arrivalCity, departureDate.plusDays(1), earliestSecondLegDeparture));
            } else {
                // 回退到原有方法
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

                // 关键修复：允许相同城市不同机场间的连接
                boolean canConnect = false;
                boolean sameCityDifferentAirport = false;
                
                if (firstLeg.getArrivalAirportId().equals(secondLeg.getDepartureAirportId())) {
                    // 同一机场连接
                    canConnect = true;
                } else if (firstLeg.getArrivalAirport() != null && 
                          secondLeg.getDepartureAirport() != null &&
                          firstLeg.getArrivalAirport().getCity() != null && 
                          secondLeg.getDepartureAirport().getCity() != null) {
                    // 检查是否为同一城市不同机场
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

                // 动态衔接时间：同机场>=1小时，不同机场同城市>=2小时
                long requiredConnectionMinutes = sameCityDifferentAirport ? 120 : 60; // 2小时 vs 1小时
                Instant minimumSecondLegDeparture = firstLegArrival.plusSeconds(requiredConnectionMinutes * 60);
                
                if (!secondLeg.getDepartureTimeUtc().isBefore(minimumSecondLegDeparture)) {
                    List<Flight> flights = List.of(firstLeg, secondLeg);
                    ConnectingFlight connectingFlight = new ConnectingFlight(flights);
                    
                    // 为同城不同机场添加标记
                    if (sameCityDifferentAirport) {
                        connectingFlight.setType("CONNECTING_INTERCITY_TRANSFER");
                    }
                    
                    result.add(connectingFlight);
                    String connectionType = sameCityDifferentAirport ? " (同城不同机场)" : " (同机场)";
                    System.out.println("组合成功: " + firstLeg.getFlightNumber() + " -> " +
                            secondLeg.getFlightNumber() + connectionType + ", 衔接时间 (分钟): " +
                            Duration.between(firstLegArrival, secondLeg.getDepartureTimeUtc()).toMinutes());
                    
                    // 输出转机提醒信息
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
        // Validate flight number format against airline
        validateFlightNumber(flight);
        
        flightMapper.insert(flight);
        return flight;
    }
    
    public Flight updateFlight(Flight flight) {
        // Get the existing flight to preserve certain fields if not provided
        Flight existingFlight = flightMapper.findById(flight.getId());
        if (existingFlight == null) {
            throw new RuntimeException("Flight not found with id: " + flight.getId());
        }
        
        // Validate flight number format against airline
        validateFlightNumber(flight);
        
        // If availableSeats is not provided, keep the existing value
        if (flight.getAvailableSeats() == null) {
            flight.setAvailableSeats(existingFlight.getAvailableSeats());
        }
        
        // If status is not provided, keep the existing value
        if (flight.getStatus() == null || flight.getStatus().isEmpty()) {
            flight.setStatus(existingFlight.getStatus());
        }
        
        flightMapper.update(flight);
        return flightMapper.findById(flight.getId());
    }
    
    public void deleteFlight(Long id) {
        // Check if there are any tickets for this flight
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
        
        // Update status and check if flight has departed
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
    
    /**
     * Update flight status based on current time
     * @param flight The flight to update
     */
    private void updateFlightStatusBasedOnTime(Flight flight) {
        if (flight == null) return;
        
        Instant now = Instant.now();
        String currentStatus = flight.getStatus();
        
        // Don't update manually set statuses like CANCELLED, DELAYED
        if ("CANCELLED".equals(currentStatus) || "DELAYED".equals(currentStatus)) {
            return;
        }
        
        String newStatus = null;
        
        if (flight.getArrivalTimeUtc() != null && now.isAfter(flight.getArrivalTimeUtc())) {
            // Flight has landed - 已降落
            newStatus = "已降落";
        } else if (flight.getDepartureTimeUtc() != null && now.isAfter(flight.getDepartureTimeUtc())) {
            // Flight has departed but not yet landed - 已起飞
            newStatus = "已起飞";
        } else {
            // Flight hasn't departed yet
            if (!"SCHEDULED".equals(currentStatus) && !"DELAYED".equals(currentStatus)) {
                newStatus = "SCHEDULED";
            }
        }
        
        // Update database if status changed
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            flight.setStatus(newStatus);
            flightMapper.updateStatus(flight.getId(), newStatus);
        }
    }
    
    /**
     * Update all flight statuses in batch - can be called by scheduler
     */
    public void updateAllFlightStatuses() {
        List<Flight> flights = flightMapper.findAll();
        for (Flight flight : flights) {
            updateFlightStatusBasedOnTime(flight);
        }
    }
    
    /**
     * Validate flight number format against airline code
     */
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