package com.pharmacy.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
            Long categoryId,

            List<String> ingredients
    ) {}

    public record MedicineResponse(
            Long id,
            String code,
            String name,
            BigDecimal price,
            int stockQty,
            Long categoryId,
            String categoryName,
            List<String> ingredients
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

    // ====== Activity Log ======

    public record ActivityLogResponse(
            Long id,
            String action,
            String entityType,
            Long entityId,
            String description,
            LocalDateTime occurredAt
    ) {}

    public record LogStatsRequest(
            @NotNull(message = "From date is required")
            LocalDateTime from,

            @NotNull(message = "To date is required")
            LocalDateTime to,

            String groupBy  // "month" or "day"
    ) {}

    public record ActionCount(
            String action,
            int count
    ) {}

    public record PeriodActionCount(
            String period,
            String action,
            int count
    ) {}

    public record TopMedicine(
            Long entityId,
            String medicineName,
            int movementCount,
            int totalIn,
            int totalOut
    ) {}

    public record LogStats(
            LocalDateTime from,
            LocalDateTime to,
            List<ActionCount> summary,
            List<PeriodActionCount> breakdown,
            List<TopMedicine> topMedicines
    ) {}

    // ====== Search ======

    public record SearchRequest(
            String query
    ) {}

    // ====== Generic API wrapper ======

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