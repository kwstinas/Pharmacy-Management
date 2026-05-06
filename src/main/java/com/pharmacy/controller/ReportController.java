package com.pharmacy.controller;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final StockService stockService;

    public ReportController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock-summary")
    public ResponseEntity<ApiResponse<StockSummary>> stockSummary() {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getStockSummary()));
    }

    @GetMapping("/category-stats")
    public ResponseEntity<ApiResponse<List<CategoryStats>>> categoryStats() {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getCategoryStats()));
    }

    @GetMapping("/monthly-movements")
    public ResponseEntity<ApiResponse<List<MovementSummary>>> monthlyMovements(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(ApiResponse.ok(
                stockService.getMonthlySummary(months)));
    }
}