package com.pharmacy.controller;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.LogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getRecent(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(logService.getRecentLogs(limit)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(
                logService.getLogsByDateRange(from, to)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<LogStats>> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "month") String groupBy) {
        return ResponseEntity.ok(ApiResponse.ok(
                logService.getStats(from, to, groupBy)));
    }
}