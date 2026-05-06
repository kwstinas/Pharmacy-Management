package com.pharmacy.controller;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.StockService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @PostMapping("/movement")
    public ResponseEntity<ApiResponse<StockMovementResponse>> recordMovement(
            @Valid @RequestBody StockMovementRequest req) {
        StockMovementResponse result = service.recordMovement(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stock movement recorded", result));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getMovements(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(service.getMovements(limit)));
    }

    @GetMapping("/movements/medicine/{medicineId}")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getByMedicine(
            @PathVariable Long medicineId) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getMovementsByMedicine(medicineId)));
    }

    @GetMapping("/movements/range")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getMovementsByDateRange(from, to)));
    }
}