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
import com.pharmacy.security.AuthHelper;
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

    public StockService(StockMovementRepository movementRepo, MedicineRepository medicineRepo,
                        ActivityLogRepository logRepo) {
        this.movementRepo = movementRepo;
        this.medicineRepo = medicineRepo;
        this.logRepo = logRepo;
    }

    @Transactional
    public StockMovementResponse recordMovement(StockMovementRequest req) {
        Long uid = AuthHelper.getCurrentUserId();
        Medicine medicine = medicineRepo.findById(req.medicineId(), uid)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + req.medicineId()));
        MovementType type = MovementType.valueOf(req.type());
        int currentQty = medicine.getStockQty();
        int newQty;
        if (type == MovementType.IN) {
            newQty = currentQty + req.quantity();
        } else {
            if (req.quantity() > currentQty) {
                throw new BusinessException("Insufficient stock. Available: " + currentQty + ", requested: " + req.quantity());
            }
            newQty = currentQty - req.quantity();
        }
        medicineRepo.updateStock(medicine.getId(), newQty, uid);
        StockMovement sm = new StockMovement();
        sm.setMedicineId(req.medicineId());
        sm.setType(type);
        sm.setQuantity(req.quantity());
        sm.setOccurredAt(LocalDateTime.now());
        sm.setNote(req.note());
        sm = movementRepo.save(sm, uid);
        logRepo.log(type == MovementType.IN ? "STOCK_IN" : "STOCK_OUT", "MEDICINE", medicine.getId(),
                medicine.getName() + " x" + req.quantity(), uid);
        return new StockMovementResponse(sm.getId(), sm.getMedicineId(), medicine.getName(),
                sm.getType().name(), sm.getQuantity(), sm.getOccurredAt(), sm.getNote());
    }

    public List<StockMovementResponse> getMovements(int limit) {
        Long uid = AuthHelper.getCurrentUserId();
        return movementRepo.findAll(limit, uid).stream().map(this::toResponse).toList();
    }

    public List<StockMovementResponse> getMovementsByMedicine(Long medicineId) {
        Long uid = AuthHelper.getCurrentUserId();
        return movementRepo.findByMedicine(medicineId, uid).stream().map(this::toResponse).toList();
    }

    public List<StockMovementResponse> getMovementsByDateRange(LocalDateTime from, LocalDateTime to) {
        Long uid = AuthHelper.getCurrentUserId();
        return movementRepo.findByDateRange(from, to, uid).stream().map(this::toResponse).toList();
    }

    public StockSummary getStockSummary() {
        Long uid = AuthHelper.getCurrentUserId();
        long total = medicineRepo.count(uid);
        long outOfStock = medicineRepo.countOutOfStock(uid);
        long lowStock = medicineRepo.countLowStock(10, uid);
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Medicine m : medicineRepo.findAll(uid)) {
            totalValue = totalValue.add(m.getPrice().multiply(BigDecimal.valueOf(m.getStockQty())));
        }
        return new StockSummary(total, outOfStock, lowStock, totalValue);
    }

    public List<CategoryStats> getCategoryStats() {
        Long uid = AuthHelper.getCurrentUserId();
        return movementRepo.categoryStats(uid).stream()
                .map(row -> new CategoryStats(
                        ((Number) row.get("category_id")).longValue(),
                        (String) row.get("category_name"),
                        ((Number) row.get("medicine_count")).intValue(),
                        ((Number) row.get("total_stock")).intValue(),
                        (BigDecimal) row.get("total_value")
                )).toList();
    }

    public List<MovementSummary> getMonthlySummary(int months) {
        Long uid = AuthHelper.getCurrentUserId();
        return movementRepo.monthlySummary(months, uid).stream()
                .map(row -> new MovementSummary(
                        (String) row.get("period"),
                        ((Number) row.get("total_in")).intValue(),
                        ((Number) row.get("total_out")).intValue(),
                        ((Number) row.get("total_in")).intValue() - ((Number) row.get("total_out")).intValue()
                )).toList();
    }

    private StockMovementResponse toResponse(StockMovement sm) {
        return new StockMovementResponse(sm.getId(), sm.getMedicineId(), sm.getMedicineName(),
                sm.getType().name(), sm.getQuantity(), sm.getOccurredAt(), sm.getNote());
    }
}