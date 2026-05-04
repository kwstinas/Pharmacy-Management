package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.model.MedCategory;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.security.AuthHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repo;

    @Mock
    private ActivityLogRepository logRepo;

    @InjectMocks
    private CategoryService service;

    @Test
    @DisplayName("Create category should succeed with unique name")
    void create_uniqueName_shouldSucceed() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findByName("Antibiotics", 1L)).thenReturn(Optional.empty());

            MedCategory saved = new MedCategory(1L, "Antibiotics", "Anti-bacterial drugs");
            when(repo.save(any(), eq(1L))).thenReturn(saved);

            CategoryResponse result = service.create(new CategoryRequest("Antibiotics", "Anti-bacterial drugs"));

            assertEquals("Antibiotics", result.name());
            assertEquals(1L, result.id());
            verify(logRepo).log(eq("CREATE"), eq("CATEGORY"), eq(1L), eq("Antibiotics"), eq(1L));
        }
    }

    @Test
    @DisplayName("Create category with duplicate name should throw")
    void create_duplicateName_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findByName("Antibiotics", 1L))
                    .thenReturn(Optional.of(new MedCategory(1L, "Antibiotics", null)));

            assertThrows(BusinessException.class,
                    () -> service.create(new CategoryRequest("Antibiotics", "desc")));

            verify(repo, never()).save(any(), anyLong());
        }
    }

    @Test
    @DisplayName("Delete category with medicines should throw")
    void delete_withMedicines_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findById(1L, 1L))
                    .thenReturn(Optional.of(new MedCategory(1L, "Antibiotics", null)));
            when(repo.countMedicines(1L, 1L)).thenReturn(5);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(1L));

            assertTrue(ex.getMessage().contains("5 medicines"));
            verify(repo, never()).deleteById(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("Delete empty category should succeed")
    void delete_emptyCategory_shouldSucceed() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findById(1L, 1L))
                    .thenReturn(Optional.of(new MedCategory(1L, "Empty Cat", null)));
            when(repo.countMedicines(1L, 1L)).thenReturn(0);
            when(repo.deleteById(1L, 1L)).thenReturn(1);

            assertDoesNotThrow(() -> service.delete(1L));
            verify(repo).deleteById(1L, 1L);
            verify(logRepo).log(eq("DELETE"), eq("CATEGORY"), eq(1L), eq("Empty Cat"), eq(1L));
        }
    }

    @Test
    @DisplayName("Find non-existent category should throw")
    void findById_notFound_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findById(99L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.findById(99L));
        }
    }

    @Test
    @DisplayName("Find all categories should return list")
    void findAll_shouldReturnList() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(repo.findAll(1L)).thenReturn(List.of(
                    new MedCategory(1L, "Antibiotics", null),
                    new MedCategory(2L, "Painkillers", null)
            ));
            when(repo.countMedicines(anyLong(), eq(1L))).thenReturn(3);

            List<CategoryResponse> result = service.findAll();

            assertEquals(2, result.size());
            assertEquals("Antibiotics", result.get(0).name());
            assertEquals(3, result.get(0).medicineCount());
        }
    }
}