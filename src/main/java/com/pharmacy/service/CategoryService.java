package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.MedCategory;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.security.AuthHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final ActivityLogRepository logRepo;

    public CategoryService(CategoryRepository repo, ActivityLogRepository logRepo) {
        this.repo = repo;
        this.logRepo = logRepo;
    }

    public List<CategoryResponse> findAll() {
        Long uid = AuthHelper.getCurrentUserId();
        return repo.findAll(uid).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription(), repo.countMedicines(c.getId(), uid)))
                .toList();
    }

    public CategoryResponse findById(Long id) {
        Long uid = AuthHelper.getCurrentUserId();
        MedCategory c = repo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), repo.countMedicines(c.getId(), uid));
    }

    public CategoryResponse create(CategoryRequest req) {
        Long uid = AuthHelper.getCurrentUserId();
        repo.findByName(req.name(), uid).ifPresent(e -> {
            throw new BusinessException("Category already exists: " + req.name());
        });
        MedCategory c = new MedCategory();
        c.setName(req.name());
        c.setDescription(req.description());
        c = repo.save(c, uid);
        logRepo.log("CREATE", "CATEGORY", c.getId(), c.getName(), uid);
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), 0);
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        Long uid = AuthHelper.getCurrentUserId();
        MedCategory c = repo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        c.setName(req.name());
        c.setDescription(req.description());
        repo.update(c, uid);
        logRepo.log("UPDATE", "CATEGORY", id, c.getName(), uid);
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), repo.countMedicines(id, uid));
    }

    public void delete(Long id) {
        Long uid = AuthHelper.getCurrentUserId();
        MedCategory c = repo.findById(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        int medicines = repo.countMedicines(id, uid);
        if (medicines > 0) {
            throw new BusinessException("Cannot delete category with " + medicines + " medicines.");
        }
        repo.deleteById(id, uid);
        logRepo.log("DELETE", "CATEGORY", id, c.getName(), uid);
    }
}