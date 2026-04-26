package com.pharmacy.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dtos {

    // ====== Category ======

    public record CategoryRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255)
            String name,

            String description
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String description,
            int medicineCount
    ) {}

    // ====== Medicine ======

    public record MedicineRequest(
            @NotBlank(message = "Code is required")
            @Size(max = 64)
            String code,

            @NotBlank(message = "Name is required")
            @Size(max = 255)
            String name,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.00", message = "Price must be >= 0")
            BigDecimal price,

            @NotNull(message = "Category ID is required")
            Long categoryId
    ) {}

    public record MedicineResponse(
            Long id,
            String code,
            String name,
            BigDecimal price,
            int stockQty,
            Long categoryId,
            String categoryName
    ) {}

    // ====== Stock Movement ======

    public record StockMovementRequest(
            @NotNull(message = "Medicine ID is required")
            Long medicineId,

            @NotBlank(message = "Type is required (IN or OUT)")
            @Pattern(regexp = "IN|OUT", message = "Type must be IN or OUT")
            String type,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity,

            String note
    ) {}

    public record StockMovementResponse(
            Long id,
            Long medicineId,
            String medicineName,
            String type,
            int quantity,
            LocalDateTime occurredAt,
            String note
    ) {}

    // ====== Reports ======

    public record StockSummary(
            long totalMedicines,
            long outOfStock,
            long lowStock,
            BigDecimal totalStockValue
    ) {}

    public record CategoryStats(
            Long categoryId,
            String categoryName,
            int medicineCount,
            int totalStock,
            BigDecimal totalValue
    ) {}

    public record MovementSummary(
            String period,
            int totalIn,
            int totalOut,
            int netChange
    ) {}

    // ====== Ενιαίο API Response wrapper ======

    public record ApiResponse<T>(
            boolean success,
            String message,
            T data
    ) {
        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, "OK", data);
        }
        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }
}