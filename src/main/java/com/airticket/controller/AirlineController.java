package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.model.Airline;
import com.airticket.service.AirlineService;
import com.airticket.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@CrossOrigin(origins = "*")
public class AirlineController {

    @Autowired
    private AirlineService airlineService;
    
    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Airline>>> getAllAirlines() {
        try {
            List<Airline> airlines = airlineService.getAllActiveAirlines();
            return ResponseEntity.ok(ApiResponse.success(airlines));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to load airlines: " + e.getMessage()));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Airline>>> getAllAirlinesForAdmin() {
        try {
            List<Airline> airlines = airlineService.getAllAirlines();
            return ResponseEntity.ok(ApiResponse.success(airlines));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to load airlines: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Airline>> getAirlineById(@PathVariable Long id) {
        try {
            Airline airline = airlineService.getAirlineById(id);
            if (airline == null) {
                return ResponseEntity.ok(ApiResponse.error("Airline not found"));
            }
            return ResponseEntity.ok(ApiResponse.success(airline));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to load airline: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Airline>> createAirline(@RequestBody Airline airline) {
        try {
            Airline created = airlineService.createAirline(airline);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to create airline: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Airline>> updateAirline(@PathVariable Long id, @RequestBody Airline airline) {
        try {
            airline.setId(id);
            Airline updated = airlineService.updateAirline(airline);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to update airline: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAirline(@PathVariable Long id) {
        try {
            airlineService.deleteAirline(id);
            return ResponseEntity.ok(ApiResponse.success("Airline deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to delete airline: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateAirlineStatus(@PathVariable Long id, @RequestParam Boolean active) {
        try {
            airlineService.updateActiveStatus(id, active);
            return ResponseEntity.ok(ApiResponse.success("Airline status updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to update airline status: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadAirlineLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            Airline airline = airlineService.getAirlineById(id);
            if (airline == null) {
                return ResponseEntity.ok(ApiResponse.error("Airline not found"));
            }

            
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.ok(ApiResponse.error("Only image files are allowed"));
            }

            
            String logoUrl = fileStorageService.storeFile(file, "airlines");
            
            
            airline.setLogoUrl(logoUrl);
            airlineService.updateAirline(airline);

            return ResponseEntity.ok(ApiResponse.success(logoUrl));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to upload logo: " + e.getMessage()));
        }
    }

    @GetMapping("/validate-flight-number")
    public ResponseEntity<ApiResponse<Boolean>> validateFlightNumber(@RequestParam String flightNumber, @RequestParam String airlineCode) {
        try {
            boolean isValid = airlineService.validateFlightNumber(flightNumber, airlineCode);
            return ResponseEntity.ok(ApiResponse.success(isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to validate flight number: " + e.getMessage()));
        }
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> initializeDefaultAirlines() {
        try {
            airlineService.initializeDefaultAirlines();
            return ResponseEntity.ok(ApiResponse.success("Default airlines initialized successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to initialize airlines: " + e.getMessage()));
        }
    }
}