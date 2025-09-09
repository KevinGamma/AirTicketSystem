package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.model.AircraftModel;
import com.airticket.service.AircraftModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft-models")
@CrossOrigin(origins = "*")
public class AircraftModelController {

    @Autowired
    private AircraftModelService aircraftModelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AircraftModel>>> getAllAircraftModels() {
        List<AircraftModel> models = aircraftModelService.getAllAircraftModels();
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<AircraftModel>> getAircraftModelByCode(@PathVariable String code) {
        AircraftModel model = aircraftModelService.getByCode(code);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(model));
    }
}