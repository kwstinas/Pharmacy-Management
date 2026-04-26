package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.model.MedCategory;
import com.pharmacy.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
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
        // Έλεγχος: υπάρχει ήδη αυτό το όνομα;
        repo.findByName(req.name()).ifPresent(existing -> {
            throw new BusinessException("Category already exists: " + req.name());
        });

        MedCategory c = new MedCategory();
        c.setName(req.name());
        c.setDescription(req.description());
        c = repo.save(c);

        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), 0);
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        MedCategory c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        c.setName(req.name());
        c.setDescription(req.description());
        repo.update(c);

        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                repo.countMedicines(id));
    }

    public void delete(Long id) {
        // Έλεγχος: έχει φάρμακα μέσα;
        int medicines = repo.countMedicines(id);
        if (medicines > 0) {
            throw new BusinessException(
                    "Cannot delete category with " + medicines + " medicines. Move them first.");
        }
        int rows = repo.deleteById(id);
        if (rows == 0) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
    }
}