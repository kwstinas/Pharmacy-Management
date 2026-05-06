package com.pharmacy;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.MedicineIngredient;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.IngredientRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.security.AuthHelper;
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

    public MedicineService(MedicineRepository medicineRepo, CategoryRepository categoryRepo,
                           IngredientRepository ingredientRepo, ActivityLogRepository logRepo) {
        this.medicineRepo = medicineRepo;
        this.categoryRepo = categoryRepo;
        this.ingredientRepo = ingredientRepo;
        this.logRepo = logRepo;
    }

    public List<MedicineResponse> findAll() {
        Long uid = AuthHelper.getCurrentUserId();
        return medicineRepo.findAll(uid).stream().map(this::toResponse).toList();
    }

    public MedicineResponse findById(Long id) {
        Long uid = AuthHelper.getCurrentUserId();
        Medicine m = medicineRepo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
        return toResponse(m);
    }

    public List<MedicineResponse> findByCategory(Long categoryId) {
        Long uid = AuthHelper.getCurrentUserId();
        return medicineRepo.findByCategory(categoryId, uid).stream().map(this::toResponse).toList();
    }

    public List<MedicineResponse> findLowStock(int threshold) {
        Long uid = AuthHelper.getCurrentUserId();
        return medicineRepo.findLowStock(threshold, uid).stream().map(this::toResponse).toList();
    }

    public List<MedicineResponse> search(String keyword) {
        Long uid = AuthHelper.getCurrentUserId();
        List<Medicine> byNameCode = medicineRepo.search(keyword, uid);
        List<Long> ingredientMatches = ingredientRepo.findMedicineIdsByIngredient(keyword, uid);
        List<Long> alreadyFound = byNameCode.stream().map(Medicine::getId).toList();
        List<Medicine> combined = new ArrayList<>(byNameCode);
        for (Long medId : ingredientMatches) {
            if (!alreadyFound.contains(medId)) {
                medicineRepo.findById(medId, uid).ifPresent(combined::add);
            }
        }
        return combined.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MedicineResponse create(MedicineRequest req) {
        Long uid = AuthHelper.getCurrentUserId();
        categoryRepo.findById(req.categoryId(), uid)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.categoryId()));
        medicineRepo.findByCode(req.code(), uid).ifPresent(e -> {
            throw new BusinessException("Medicine code already exists: " + req.code());
        });
        Medicine m = new Medicine();
        m.setCode(req.code());
        m.setName(req.name());
        m.setPrice(req.price());
        m.setStockQty(0);
        m.setCategoryId(req.categoryId());
        m = medicineRepo.save(m, uid);
        if (req.ingredients() != null && !req.ingredients().isEmpty()) {
            ingredientRepo.saveAll(m.getId(), req.ingredients(), uid);
        }
        logRepo.log("CREATE", "MEDICINE", m.getId(), m.getName(), uid);
        return medicineRepo.findById(m.getId(), uid).map(this::toResponse).orElseThrow();
    }

    @Transactional
    public MedicineResponse update(Long id, MedicineRequest req) {
        Long uid = AuthHelper.getCurrentUserId();
        Medicine m = medicineRepo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
        categoryRepo.findById(req.categoryId(), uid)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.categoryId()));
        m.setCode(req.code());
        m.setName(req.name());
        m.setPrice(req.price());
        m.setCategoryId(req.categoryId());
        medicineRepo.update(m, uid);
        if (req.ingredients() != null) {
            ingredientRepo.saveAll(id, req.ingredients(), uid);
        }
        logRepo.log("UPDATE", "MEDICINE", id, m.getName(), uid);
        return medicineRepo.findById(id, uid).map(this::toResponse).orElseThrow();
    }

    @Transactional
    public void delete(Long id) {
        Long uid = AuthHelper.getCurrentUserId();
        Medicine m = medicineRepo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
        ingredientRepo.deleteByMedicine(id);
        medicineRepo.deleteById(id, uid);
        logRepo.log("DELETE", "MEDICINE", id, m.getName(), uid);
    }

    private MedicineResponse toResponse(Medicine m) {
        List<String> ingredients = ingredientRepo.findByMedicine(m.getId())
                .stream().map(MedicineIngredient::getIngredientName).toList();
        return new MedicineResponse(m.getId(), m.getCode(), m.getName(), m.getPrice(),
                m.getStockQty(), m.getCategoryId(), m.getCategoryName(), ingredients);
    }
}