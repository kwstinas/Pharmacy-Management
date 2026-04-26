package com.pharmacy.controller;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService service;

    public MedicineController(MedicineService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MedicineResponse>>> search(
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(service.search(q)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<MedicineResponse>>> getByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByCategory(categoryId)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<MedicineResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(ApiResponse.ok(service.findLowStock(threshold)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicineResponse>> create(
            @Valid @RequestBody MedicineRequest req) {
        MedicineResponse created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Medicine created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MedicineRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Medicine updated", service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Medicine deleted", null));
    }
}