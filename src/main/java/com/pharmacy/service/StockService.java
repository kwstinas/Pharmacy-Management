package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.StockMovement;
import com.pharmacy.model.StockMovement.MovementType;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final StockMovementRepository movementRepo;
    private final MedicineRepository medicineRepo;
    private final ActivityLogRepository logRepo;

    public StockService(StockMovementRepository movementRepo,
                        MedicineRepository medicineRepo,
                        ActivityLogRepository logRepo) {
        this.movementRepo = movementRepo;
        this.medicineRepo = medicineRepo;
        this.logRepo = logRepo;
    }

    @Transactional
    public StockMovementResponse recordMovement(StockMovementRequest req) {
        Medicine medicine = medicineRepo.findById(req.medicineId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicine not found: " + req.medicineId()));

        MovementType type = MovementType.valueOf(req.type());
        int currentQty = medicine.getStockQty();
        int newQty;

        if (type == MovementType.IN) {
            newQty = currentQty + req.quantity();
        } else {
            if (req.quantity() > currentQty) {
                throw new BusinessException(
                        "Insufficient stock. Available: " + currentQty
                                + ", requested: " + req.quantity());
            }
            newQty = currentQty - req.quantity();
        }

        medicineRepo.updateStock(medicine.getId(), newQty);

        StockMovement sm = new StockMovement();
        sm.setMedicineId(req.medicineId());
        sm.setType(type);
        sm.setQuantity(req.quantity());
        sm.setOccurredAt(LocalDateTime.now());
        sm.setNote(req.note());
        sm = movementRepo.save(sm);

        // Log
        logRepo.log(
                type == MovementType.IN ? "STOCK_IN" : "STOCK_OUT",
                "MEDICINE", medicine.getId(),
                medicine.getName() + " x" + req.quantity());

        return new StockMovementResponse(
                sm.getId(), sm.getMedicineId(), medicine.getName(),
                sm.getType().name(), sm.getQuantity(),
                sm.getOccurredAt(), sm.getNote());
    }

    public List<StockMovementResponse> getMovements(int limit) {
        return movementRepo.findAll(limit).stream()
                .map(this::toResponse).toList();
    }

    public List<StockMovementResponse> getMovementsByMedicine(Long medicineId) {
        return movementRepo.findByMedicine(medicineId).stream()
                .map(this::toResponse).toList();
    }

    public List<StockMovementResponse> getMovementsByDateRange(
            LocalDateTime from, LocalDateTime to) {
        return movementRepo.findByDateRange(from, to).stream()
                .map(this::toResponse).toList();
    }

    public StockSummary getStockSummary() {
        long total = medicineRepo.count();
        long outOfStock = medicineRepo.countOutOfStock();
        long lowStock = medicineRepo.countLowStock(10);

        BigDecimal totalValue = BigDecimal.ZERO;
        List<Medicine> all = medicineRepo.findAll();
        for (Medicine m : all) {
            totalValue = totalValue.add(
                    m.getPrice().multiply(BigDecimal.valueOf(m.getStockQty())));
        }

        return new StockSummary(total, outOfStock, lowStock, totalValue);
    }

    public List<CategoryStats> getCategoryStats() {
        return movementRepo.categoryStats().stream()
                .map(row -> new CategoryStats(
                        ((Number) row.get("category_id")).longValue(),
                        (String) row.get("category_name"),
                        ((Number) row.get("medicine_count")).intValue(),
                        ((Number) row.get("total_stock")).intValue(),
                        (BigDecimal) row.get("total_value")
                )).toList();
    }

    public List<MovementSummary> getMonthlySummary(int months) {
        return movementRepo.monthlySummary(months).stream()
                .map(row -> new MovementSummary(
                        (String) row.get("period"),
                        ((Number) row.get("total_in")).intValue(),
                        ((Number) row.get("total_out")).intValue(),
                        ((Number) row.get("total_in")).intValue()
                                - ((Number) row.get("total_out")).intValue()
                )).toList();
    }

    private StockMovementResponse toResponse(StockMovement sm) {
        return new StockMovementResponse(
                sm.getId(), sm.getMedicineId(), sm.getMedicineName(),
                sm.getType().name(), sm.getQuantity(),
                sm.getOccurredAt(), sm.getNote());
    }
}