package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.MedCategory;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.ActivityLogRepository;
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
        return repo.findAll().stream()
                .map(c -> new CategoryResponse(
                        c.getId(), c.getName(), c.getDescription(),
                        repo.countMedicines(c.getId())))
                .toList();
    }

    public CategoryResponse findById(Long id) {
        MedCategory c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                repo.countMedicines(c.getId()));
    }

    public CategoryResponse create(CategoryRequest req) {
        repo.findByName(req.name()).ifPresent(existing -> {
            throw new BusinessException("Category already exists: " + req.name());
        });

        MedCategory c = new MedCategory();
        c.setName(req.name());
        c.setDescription(req.description());
        c = repo.save(c);

        logRepo.log("CREATE", "CATEGORY", c.getId(), c.getName());

        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), 0);
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        MedCategory c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        c.setName(req.name());
        c.setDescription(req.description());
        repo.update(c);

        logRepo.log("UPDATE", "CATEGORY", id, c.getName());

        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                repo.countMedicines(id));
    }

    public void delete(Long id) {
        MedCategory c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        int medicines = repo.countMedicines(id);
        if (medicines > 0) {
            throw new BusinessException(
                    "Cannot delete category with " + medicines + " medicines. Move them first.");
        }
        repo.deleteById(id);

        logRepo.log("DELETE", "CATEGORY", id, c.getName());
    }
}