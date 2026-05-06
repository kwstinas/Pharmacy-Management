package com.pharmacy.service;

import com.pharmacy.MedicineService;
import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.model.MedCategory;
import com.pharmacy.model.Medicine;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.IngredientRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.security.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private IngredientRepository ingredientRepo;

    @Mock
    private ActivityLogRepository logRepo;

    @InjectMocks
    private MedicineService service;

    private Medicine testMedicine;
    private MedCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new MedCategory(1L, "Painkillers", "Pain relief");

        testMedicine = new Medicine();
        testMedicine.setId(1L);
        testMedicine.setCode("ASP-500");
        testMedicine.setName("Aspirin 500mg");
        testMedicine.setPrice(new BigDecimal("3.50"));
        testMedicine.setStockQty(50);
        testMedicine.setCategoryId(1L);
        testMedicine.setCategoryName("Painkillers");
    }

    @Test
    @DisplayName("Create medicine should succeed with valid data")
    void create_validData_shouldSucceed() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(categoryRepo.findById(1L, 1L)).thenReturn(Optional.of(testCategory));
            when(medicineRepo.findByCode("ASP-500", 1L)).thenReturn(Optional.empty());

            Medicine saved = new Medicine();
            saved.setId(1L);
            saved.setCode("ASP-500");
            saved.setName("Aspirin 500mg");
            saved.setPrice(new BigDecimal("3.50"));
            saved.setStockQty(0);
            saved.setCategoryId(1L);
            when(medicineRepo.save(any(), eq(1L))).thenReturn(saved);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));
            when(ingredientRepo.findByMedicine(1L)).thenReturn(Collections.emptyList());

            MedicineRequest req = new MedicineRequest("ASP-500", "Aspirin 500mg",
                    new BigDecimal("3.50"), 1L, List.of("Acetylsalicylic acid"));

            MedicineResponse result = service.create(req);

            assertEquals("ASP-500", result.code());
            assertEquals("Aspirin 500mg", result.name());
            verify(ingredientRepo).saveAll(eq(1L), eq(List.of("Acetylsalicylic acid")), eq(1L));
            verify(logRepo).log(eq("CREATE"), eq("MEDICINE"), eq(1L), eq("Aspirin 500mg"), eq(1L));
        }
    }

    @Test
    @DisplayName("Create medicine with duplicate code should throw")
    void create_duplicateCode_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(categoryRepo.findById(1L, 1L)).thenReturn(Optional.of(testCategory));
            when(medicineRepo.findByCode("ASP-500", 1L)).thenReturn(Optional.of(testMedicine));

            MedicineRequest req = new MedicineRequest("ASP-500", "Aspirin 500mg",
                    new BigDecimal("3.50"), 1L, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains("ASP-500"));
            verify(medicineRepo, never()).save(any(), anyLong());
        }
    }

    @Test
    @DisplayName("Create medicine with non-existent category should throw")
    void create_invalidCategory_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(categoryRepo.findById(99L, 1L)).thenReturn(Optional.empty());

            MedicineRequest req = new MedicineRequest("NEW-001", "New Drug",
                    new BigDecimal("5.00"), 99L, null);

            assertThrows(ResourceNotFoundException.class,
                    () -> service.create(req));

            verify(medicineRepo, never()).save(any(), anyLong());
        }
    }

    @Test
    @DisplayName("Delete medicine should succeed and clean up ingredients")
    void delete_shouldCleanIngredients() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));
            when(medicineRepo.deleteById(1L, 1L)).thenReturn(1);

            assertDoesNotThrow(() -> service.delete(1L));

            verify(ingredientRepo).deleteByMedicine(1L);
            verify(medicineRepo).deleteById(1L, 1L);
            verify(logRepo).log(eq("DELETE"), eq("MEDICINE"), eq(1L), eq("Aspirin 500mg"), eq(1L));
        }
    }

    @Test
    @DisplayName("Delete non-existent medicine should throw")
    void delete_notFound_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(99L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.delete(99L));

            verify(ingredientRepo, never()).deleteByMedicine(anyLong());
        }
    }

    @Test
    @DisplayName("Search should combine name and ingredient results")
    void search_shouldCombineResults() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);

            // Medicine found by name search
            Medicine byName = new Medicine();
            byName.setId(1L);
            byName.setCode("ASP-500");
            byName.setName("Aspirin 500mg");
            byName.setPrice(new BigDecimal("3.50"));
            byName.setStockQty(50);
            byName.setCategoryId(1L);
            byName.setCategoryName("Painkillers");

            // Different medicine found by ingredient search
            Medicine byIngredient = new Medicine();
            byIngredient.setId(2L);
            byIngredient.setCode("PAN-500");
            byIngredient.setName("Panadol");
            byIngredient.setPrice(new BigDecimal("4.00"));
            byIngredient.setStockQty(30);
            byIngredient.setCategoryId(1L);
            byIngredient.setCategoryName("Painkillers");

            when(medicineRepo.search("paracetamol", 1L)).thenReturn(List.of(byName));
            when(ingredientRepo.findMedicineIdsByIngredient("paracetamol", 1L)).thenReturn(List.of(2L));
            when(medicineRepo.findById(2L, 1L)).thenReturn(Optional.of(byIngredient));
            when(ingredientRepo.findByMedicine(anyLong())).thenReturn(Collections.emptyList());

            List<MedicineResponse> results = service.search("paracetamol");

            assertEquals(2, results.size());
            assertEquals("ASP-500", results.get(0).code());
            assertEquals("PAN-500", results.get(1).code());
        }
    }

    @Test
    @DisplayName("Search should not duplicate results")
    void search_shouldNotDuplicate() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);

            // Same medicine found by both name AND ingredient
            when(medicineRepo.search("aspirin", 1L)).thenReturn(List.of(testMedicine));
            when(ingredientRepo.findMedicineIdsByIngredient("aspirin", 1L)).thenReturn(List.of(1L));
            when(ingredientRepo.findByMedicine(1L)).thenReturn(Collections.emptyList());

            List<MedicineResponse> results = service.search("aspirin");

            assertEquals(1, results.size());
        }
    }
}