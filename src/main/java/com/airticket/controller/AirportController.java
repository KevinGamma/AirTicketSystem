package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.service.AirportService;
import com.airticket.model.Airport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
@CrossOrigin(origins = "*")
public class AirportController {

    @Autowired
    private AirportService airportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Airport>>> getAllAirports() {
        try {
            List<Airport> airports = airportService.findAll();
            return ResponseEntity.ok(ApiResponse.success(airports));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve airports: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Airport>> getAirportById(@PathVariable Long id) {
        try {
            Airport airport = airportService.findById(id);
            if (airport == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(airport));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve airport: " + e.getMessage()));
        }
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Airport>> getAirportByCode(@PathVariable String code) {
        try {
            Airport airport = airportService.findByCode(code.toUpperCase());
            if (airport == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(airport));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve airport: " + e.getMessage()));
        }
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<Airport>>> getAirportsByCity(@PathVariable String city) {
        try {
            List<Airport> airports = airportService.findByCity(city);
            return ResponseEntity.ok(ApiResponse.success(airports));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve airports: " + e.getMessage()));
        }
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<ApiResponse<List<Airport>>> getAirportsByCountry(@PathVariable String country) {
        try {
            List<Airport> airports = airportService.findByCountry(country);
            return ResponseEntity.ok(ApiResponse.success(airports));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve airports: " + e.getMessage()));
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<String>>> getAllCountries() {
        try {
            List<String> countries = airportService.findAllCountries();
            return ResponseEntity.ok(ApiResponse.success(countries));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve countries: " + e.getMessage()));
        }
    }

    @GetMapping("/cities/{country}")
    public ResponseEntity<ApiResponse<List<String>>> getCitiesByCountry(@PathVariable String country) {
        try {
            List<String> cities = airportService.findCitiesByCountry(country);
            return ResponseEntity.ok(ApiResponse.success(cities));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve cities: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Airport>> createAirport(@RequestBody Airport airport) {
        try {
            Airport createdAirport = airportService.createAirport(airport);
            return ResponseEntity.ok(ApiResponse.success("Airport created successfully", createdAirport));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create airport: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Airport>> updateAirport(@PathVariable Long id, @RequestBody Airport airport) {
        try {
            Airport updatedAirport = airportService.updateAirport(id, airport);
            return ResponseEntity.ok(ApiResponse.success("Airport updated successfully", updatedAirport));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update airport: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAirport(@PathVariable Long id) {
        try {
            airportService.deleteAirport(id);
            return ResponseEntity.ok(ApiResponse.success("Airport deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete airport: " + e.getMessage()));
        }
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> initializeAirports() {
        try {
            airportService.initializeCommonAirports();
            return ResponseEntity.ok(ApiResponse.success("Airports initialized successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to initialize airports: " + e.getMessage()));
        }
    }
}