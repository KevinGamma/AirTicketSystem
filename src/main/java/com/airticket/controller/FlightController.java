package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.dto.FlightSearchRequest;
import com.airticket.dto.ConnectingFlightSearchRequest;
import com.airticket.dto.FlightDisplayDto;
import com.airticket.model.Flight;
import com.airticket.model.ConnectingFlight;
import com.airticket.service.FlightService;
import com.airticket.service.MessageService;
import com.airticket.mapper.FlightMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "*")
public class FlightController {

    @Autowired
    private FlightService flightService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private FlightMapper flightMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightDisplayDto>>> getAllFlights() {
        List<Flight> flights = flightService.getAllFlights();
        List<FlightDisplayDto> displayFlights = flights.stream()
            .map(FlightDisplayDto::fromFlight)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(displayFlights));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightDisplayDto>> getFlightById(@PathVariable Long id) {
        Flight flight = flightService.getFlightById(id);
        if (flight == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(FlightDisplayDto.fromFlight(flight)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FlightDisplayDto>>> searchFlightsGet(
            @RequestParam(required = false) Long originAirportId,
            @RequestParam(required = false) Long destinationAirportId,
            @RequestParam(required = false) String departureDate,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) Integer passengers) {
        
        try {
            // Map airport IDs to city names
            String departureCity = getAirportCity(originAirportId);
            String arrivalCity = getAirportCity(destinationAirportId);
            
            // Parse departure date
            java.time.LocalDate parsedDate = java.time.LocalDate.parse(departureDate);
            
            // Create FlightSearchRequest from query parameters
            FlightSearchRequest request = new FlightSearchRequest();
            request.setDepartureCity(departureCity);
            request.setArrivalCity(arrivalCity);
            request.setDepartureDate(parsedDate);
            request.setTicketType(ticketType != null ? ticketType : "ECONOMY");
            request.setPassengers(passengers != null ? passengers : 1);
            
            System.out.println("=== FLIGHT CONTROLLER DEBUG (GET) ===");
            System.out.println("Received search request from frontend:");
            System.out.println("  Origin Airport ID: " + originAirportId + " -> City: " + departureCity);
            System.out.println("  Destination Airport ID: " + destinationAirportId + " -> City: " + arrivalCity);
            System.out.println("  Departure Date: " + departureDate + " -> Parsed: " + parsedDate);
            System.out.println("  Ticket Type: " + ticketType);
            System.out.println("  Passengers: " + passengers);
            
            List<Flight> flights = flightService.searchFlights(request);
            List<FlightDisplayDto> displayFlights = flights.stream()
                .map(FlightDisplayDto::fromFlight)
                .toList();
                
            System.out.println("Returning " + displayFlights.size() + " flights to frontend");
            System.out.println("=== END FLIGHT CONTROLLER DEBUG ===");
            
            return ResponseEntity.ok(ApiResponse.success(displayFlights));
        } catch (Exception e) {
            System.err.println("Error in flight search: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }
    
    private String getAirportCity(Long airportId) {
        if (airportId == null) return null;
        
        // Simple mapping based on the data we saw earlier
        switch (airportId.intValue()) {
            case 1: return "北京";
            case 2: return "上海";
            case 3: return "广州";
            case 4: return "洛杉矶";
            case 5: return "纽约";
            case 6: return "东京";
            case 7: return "首尔";
            case 8: return "新加坡";
            default: return "未知";
        }
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<FlightDisplayDto>>> searchFlights(@Valid @RequestBody FlightSearchRequest request) {
        System.out.println("=== FLIGHT CONTROLLER DEBUG ===");
        System.out.println("Received search request from frontend:");
        System.out.println("  Departure City: '" + request.getDepartureCity() + "'");
        System.out.println("  Arrival City: '" + request.getArrivalCity() + "'");
        System.out.println("  Departure Date: " + request.getDepartureDate());
        System.out.println("Request JSON equivalent: " + request.toString());
        
        List<Flight> flights = flightService.searchFlights(request);
        List<FlightDisplayDto> displayFlights = flights.stream()
            .map(FlightDisplayDto::fromFlight)
            .toList();
            
        System.out.println("Returning " + displayFlights.size() + " flights to frontend");
        System.out.println("=== END FLIGHT CONTROLLER DEBUG ===");
        
        return ResponseEntity.ok(ApiResponse.success(displayFlights));
    }

    @PostMapping("/search/connecting")
    public ResponseEntity<ApiResponse<List<ConnectingFlight>>> searchConnectingFlights(@Valid @RequestBody ConnectingFlightSearchRequest request) {
        try {
            List<ConnectingFlight> flights = flightService.searchConnectingFlights(request);
            System.out.println("成功找到 " + flights.size() + " 个航班组合，准备返回给前端");
            
            // Clean the flights to remove circular references before serialization
            List<ConnectingFlight> cleanedFlights = flights.stream().map(cf -> {
                ConnectingFlight cleaned = new ConnectingFlight();
                cleaned.setFlights(cf.getFlights().stream().map(f -> {
                    Flight cleanedFlight = new Flight();
                    cleanedFlight.setId(f.getId());
                    cleanedFlight.setFlightNumber(f.getFlightNumber());
                    cleanedFlight.setAirlineId(f.getAirlineId());
                    cleanedFlight.setDepartureAirportId(f.getDepartureAirportId());
                    cleanedFlight.setArrivalAirportId(f.getArrivalAirportId());
                    cleanedFlight.setDepartureTimeUtc(f.getDepartureTimeUtc());
                    cleanedFlight.setArrivalTimeUtc(f.getArrivalTimeUtc());
                    cleanedFlight.setTotalSeats(f.getTotalSeats());
                    cleanedFlight.setAvailableSeats(f.getAvailableSeats());
                    cleanedFlight.setPrice(f.getPrice());
                    cleanedFlight.setStatus(f.getStatus());
                    cleanedFlight.setAircraftType(f.getAircraftType());
                    cleanedFlight.setCreatedAt(f.getCreatedAt());
                    cleanedFlight.setUpdatedAt(f.getUpdatedAt());
                    
                    // Preserve airport information for frontend display
                    if (f.getDepartureAirport() != null) {
                        cleanedFlight.setDepartureAirport(f.getDepartureAirport());
                    }
                    if (f.getArrivalAirport() != null) {
                        cleanedFlight.setArrivalAirport(f.getArrivalAirport());
                    }
                    if (f.getAirline() != null) {
                        cleanedFlight.setAirline(f.getAirline());
                    }
                    
                    return cleanedFlight;
                }).toList());
                cleaned.setTotalPrice(cf.getTotalPrice());
                cleaned.setTotalDurationMinutes(cf.getTotalDurationMinutes());
                cleaned.setDepartureTimeUtc(cf.getDepartureTimeUtc());
                cleaned.setArrivalTimeUtc(cf.getArrivalTimeUtc());
                cleaned.setAvailableSeats(cf.getAvailableSeats());
                cleaned.setType(cf.getType());
                return cleaned;
            }).toList();
            
            return ResponseEntity.ok(ApiResponse.success(cleanedFlights));
        } catch (Exception e) {
            System.err.println("中转航班搜索异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("搜索中转航班时出现错误: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Flight>> createFlight(@RequestBody Flight flight) {
        try {
            Instant now = Instant.now();
            
            // Validate departure and arrival times are not in the past
            if (flight.getDepartureTimeUtc() != null && flight.getDepartureTimeUtc().isBefore(now)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.create.pastDepartureTime")));
            }
            
            if (flight.getArrivalTimeUtc() != null && flight.getArrivalTimeUtc().isBefore(now)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.create.pastArrivalTime")));
            }
            
            // Basic validation
            if (flight.getDepartureAirportId() != null && flight.getArrivalAirportId() != null 
                && flight.getDepartureAirportId().equals(flight.getArrivalAirportId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.create.sameDepartureArrival")));
            }
            
            if (flight.getDepartureTimeUtc() != null && flight.getArrivalTimeUtc() != null 
                && flight.getDepartureTimeUtc().isAfter(flight.getArrivalTimeUtc())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.create.invalidTime")));
            }
            
            Flight createdFlight = flightService.createFlight(flight);
            return ResponseEntity.ok(ApiResponse.success("Flight created successfully", createdFlight));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create flight: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Flight>> updateFlight(@PathVariable Long id, @RequestBody Flight flight) {
        try {
            flight.setId(id);
            
            // Basic validation
            if (flight.getDepartureAirportId() != null && flight.getArrivalAirportId() != null 
                && flight.getDepartureAirportId().equals(flight.getArrivalAirportId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.update.sameDepartureArrival")));
            }
            
            if (flight.getDepartureTimeUtc() != null && flight.getArrivalTimeUtc() != null 
                && flight.getDepartureTimeUtc().isAfter(flight.getArrivalTimeUtc())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.update.invalidTime")));
            }
            
            Flight updatedFlight = flightService.updateFlight(flight);
            return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("flight.update.success"), updatedFlight));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace for debugging
            return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.update.failed", e.getMessage())));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFlight(@PathVariable Long id) {
        try {
            flightService.deleteFlight(id);
            return ResponseEntity.ok(ApiResponse.success("Flight deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("flight.delete.failed", e.getMessage())));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelFlight(@PathVariable Long id, @RequestBody(required = false) String reason) {
        try {
            flightService.cancelFlight(id, reason);
            return ResponseEntity.ok(ApiResponse.success("Flight cancelled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to cancel flight: " + e.getMessage()));
        }
    }
}