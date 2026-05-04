package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.model.Medicine;
import com.pharmacy.repository.ActivityLogRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.StockMovementRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMovementRepository movementRepo;

    @Mock
    private MedicineRepository medicineRepo;

    @Mock
    private ActivityLogRepository logRepo;

    @InjectMocks
    private StockService stockService;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine();
        testMedicine.setId(1L);
        testMedicine.setCode("ASP-500");
        testMedicine.setName("Aspirin 500mg");
        testMedicine.setPrice(new BigDecimal("3.50"));
        testMedicine.setStockQty(50);
        testMedicine.setCategoryId(1L);
    }

    @Test
    @DisplayName("Stock IN should increase quantity")
    void stockIn_shouldIncreaseQuantity() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));
            when(movementRepo.save(any(), eq(1L))).thenAnswer(inv -> {
                var sm = inv.getArgument(0, com.pharmacy.model.StockMovement.class);
                sm.setId(1L);
                return sm;
            });

            StockMovementRequest req = new StockMovementRequest(1L, "IN", 20, "Delivery");
            StockMovementResponse result = stockService.recordMovement(req);

            assertEquals("IN", result.type());
            assertEquals(20, result.quantity());
            verify(medicineRepo).updateStock(1L, 70, 1L);
        }
    }

    @Test
    @DisplayName("Stock OUT should decrease quantity")
    void stockOut_shouldDecreaseQuantity() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));
            when(movementRepo.save(any(), eq(1L))).thenAnswer(inv -> {
                var sm = inv.getArgument(0, com.pharmacy.model.StockMovement.class);
                sm.setId(1L);
                return sm;
            });

            StockMovementRequest req = new StockMovementRequest(1L, "OUT", 10, "Sale");
            StockMovementResponse result = stockService.recordMovement(req);

            assertEquals("OUT", result.type());
            verify(medicineRepo).updateStock(1L, 40, 1L);
        }
    }

    @Test
    @DisplayName("Stock OUT with insufficient stock should throw exception")
    void stockOut_insufficientStock_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));

            StockMovementRequest req = new StockMovementRequest(1L, "OUT", 100, "Too much");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> stockService.recordMovement(req));

            assertTrue(ex.getMessage().contains("Insufficient stock"));
            verify(medicineRepo, never()).updateStock(anyLong(), anyInt(), anyLong());
        }
    }

    @Test
    @DisplayName("Stock movement for non-existent medicine should throw exception")
    void stockMovement_medicineNotFound_shouldThrow() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(99L, 1L)).thenReturn(Optional.empty());

            StockMovementRequest req = new StockMovementRequest(99L, "IN", 10, "Test");

            assertThrows(ResourceNotFoundException.class,
                    () -> stockService.recordMovement(req));
        }
    }

    @Test
    @DisplayName("Stock OUT of exact remaining quantity should leave zero stock")
    void stockOut_exactQuantity_shouldLeaveZero() {
        try (MockedStatic<AuthHelper> auth = mockStatic(AuthHelper.class)) {
            auth.when(AuthHelper::getCurrentUserId).thenReturn(1L);
            when(medicineRepo.findById(1L, 1L)).thenReturn(Optional.of(testMedicine));
            when(movementRepo.save(any(), eq(1L))).thenAnswer(inv -> {
                var sm = inv.getArgument(0, com.pharmacy.model.StockMovement.class);
                sm.setId(1L);
                return sm;
            });

            StockMovementRequest req = new StockMovementRequest(1L, "OUT", 50, "Sell all");
            stockService.recordMovement(req);

            verify(medicineRepo).updateStock(1L, 0, 1L);
        }
    }
}