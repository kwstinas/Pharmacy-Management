package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.Medicine;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepo;
    private final CategoryRepository categoryRepo;

    public MedicineService(MedicineRepository medicineRepo,
                           CategoryRepository categoryRepo) {
        this.medicineRepo = medicineRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<MedicineResponse> findAll() {
        return medicineRepo.findAll().stream().map(this::toResponse).toList();
    }

    public MedicineResponse findById(Long id) {
        Medicine m = medicineRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
        return toResponse(m);
    }

    public List<MedicineResponse> findByCategory(Long categoryId) {
        return medicineRepo.findByCategory(categoryId).stream()
                .map(this::toResponse).toList();
    }

    public List<MedicineResponse> findLowStock(int threshold) {
        return medicineRepo.findLowStock(threshold).stream()
                .map(this::toResponse).toList();
    }

    public List<MedicineResponse> search(String keyword) {
        return medicineRepo.search(keyword).stream()
                .map(this::toResponse).toList();
    }

    public MedicineResponse create(MedicineRequest req) {
        // Έλεγχος: υπάρχει η κατηγορία;
        categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + req.categoryId()));

        // Έλεγχος: υπάρχει ήδη αυτός ο κωδικός;
        medicineRepo.findByCode(req.code()).ifPresent(existing -> {
            throw new BusinessException("Medicine code already exists: " + req.code());
        });

        Medicine m = new Medicine();
        m.setCode(req.code());
        m.setName(req.name());
        m.setPrice(req.price());
        m.setStockQty(0); // Ξεκινάει με 0, stock μπαίνει μέσω movements
        m.setCategoryId(req.categoryId());
        m = medicineRepo.save(m);

        return medicineRepo.findById(m.getId())
                .map(this::toResponse)
                .orElseThrow();
    }

    public MedicineResponse update(Long id, MedicineRequest req) {
        Medicine m = medicineRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));

        categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + req.categoryId()));

        m.setCode(req.code());
        m.setName(req.name());
        m.setPrice(req.price());
        m.setCategoryId(req.categoryId());
        medicineRepo.update(m);

        return medicineRepo.findById(id).map(this::toResponse).orElseThrow();
    }

    public void delete(Long id) {
        int rows = medicineRepo.deleteById(id);
        if (rows == 0) {
            throw new ResourceNotFoundException("Medicine not found: " + id);
        }
    }

    // Μετατρέπει Medicine → MedicineResponse
    private MedicineResponse toResponse(Medicine m) {
        return new MedicineResponse(
                m.getId(), m.getCode(), m.getName(), m.getPrice(),
                m.getStockQty(), m.getCategoryId(), m.getCategoryName());
    }
}