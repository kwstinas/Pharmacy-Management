package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.MedicineIngredient;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.IngredientRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepo;
    private final CategoryRepository categoryRepo;
    private final IngredientRepository ingredientRepo;
    private final ActivityLogRepository logRepo;

    public MedicineService(MedicineRepository medicineRepo,
                           CategoryRepository categoryRepo,
                           IngredientRepository ingredientRepo,
                           ActivityLogRepository logRepo) {
        this.medicineRepo = medicineRepo;
        this.categoryRepo = categoryRepo;
        this.ingredientRepo = ingredientRepo;
        this.logRepo = logRepo;
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
        // Ψάχνουμε σε name, code ΚΑΙ ingredients
        List<Medicine> byNameCode = medicineRepo.search(keyword);

        // Βρες medicine IDs που έχουν αυτό το ingredient
        List<Long> ingredientMatches = ingredientRepo.findMedicineIdsByIngredient(keyword);

        // Συνδύασε αποτελέσματα (χωρίς διπλότυπα)
        List<Long> alreadyFound = byNameCode.stream().map(Medicine::getId).toList();
        List<Medicine> combined = new ArrayList<>(byNameCode);

        for (Long medId : ingredientMatches) {
            if (!alreadyFound.contains(medId)) {
                medicineRepo.findById(medId).ifPresent(combined::add);
            }
        }

        return combined.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MedicineResponse create(MedicineRequest req) {
        categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + req.categoryId()));

        medicineRepo.findByCode(req.code()).ifPresent(existing -> {
            throw new BusinessException("Medicine code already exists: " + req.code());
        });

        Medicine m = new Medicine();
        m.setCode(req.code());
        m.setName(req.name());
        m.setPrice(req.price());
        m.setStockQty(0);
        m.setCategoryId(req.categoryId());
        m = medicineRepo.save(m);

        // Αποθήκευση ingredients
        if (req.ingredients() != null && !req.ingredients().isEmpty()) {
            ingredientRepo.saveAll(m.getId(), req.ingredients());
        }

        // Log
        logRepo.log("CREATE", "MEDICINE", m.getId(), m.getName());

        return medicineRepo.findById(m.getId())
                .map(this::toResponse)
                .orElseThrow();
    }

    @Transactional
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

        // Ενημέρωση ingredients
        if (req.ingredients() != null) {
            ingredientRepo.saveAll(id, req.ingredients());
        }

        // Log
        logRepo.log("UPDATE", "MEDICINE", id, m.getName());

        return medicineRepo.findById(id).map(this::toResponse).orElseThrow();
    }

    @Transactional
    public void delete(Long id) {
        Medicine m = medicineRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));

        ingredientRepo.deleteByMedicine(id);
        medicineRepo.deleteById(id);

        // Log
        logRepo.log("DELETE", "MEDICINE", id, m.getName());
    }

    private MedicineResponse toResponse(Medicine m) {
        List<String> ingredients = ingredientRepo.findByMedicine(m.getId())
                .stream()
                .map(MedicineIngredient::getIngredientName)
                .toList();

        return new MedicineResponse(
                m.getId(), m.getCode(), m.getName(), m.getPrice(),
                m.getStockQty(), m.getCategoryId(), m.getCategoryName(),
                ingredients);
    }
}