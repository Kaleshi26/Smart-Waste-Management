// Testing WasteBinService business logic with mocked repository
package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WasteBinService business logic.
 * Tests bin management operations with mocked repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class WasteBinServiceTest {

    @Mock
    private WasteBinRepository wasteBinRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WasteBinService wasteBinService;

    private WasteBin mockBin;
    private User mockResident;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        mockResident = new User();
        mockResident.setId(1L);
        mockResident.setName("John Doe");
        mockResident.setRole(UserRole.ROLE_RESIDENT);

        mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");
        mockBin.setLocation("123 Main St");
        mockBin.setBinType(BinType.GENERAL_WASTE);
        mockBin.setCapacity(120.0);
        mockBin.setCurrentLevel(50.0);
        mockBin.setStatus(BinStatus.ACTIVE);
        mockBin.setResident(mockResident);
        mockBin.setInstallationDate(LocalDate.now());
    }

    @Test
    void getBinById_ShouldReturnBin_WhenBinExists() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));

        // Act
        WasteBin result = wasteBinService.getBinById("BIN-001");

        // Assert
        assertNotNull(result);
        assertEquals("BIN-001", result.getBinId());
        assertEquals("123 Main St", result.getLocation());
        assertEquals(BinType.GENERAL_WASTE, result.getBinType());
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
    }

    @Test
    void getBinById_ShouldThrowException_WhenBinNotFound() {
        // Arrange
        when(wasteBinRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wasteBinService.getBinById("NONEXISTENT");
        });

        assertEquals("Bin not found with ID: NONEXISTENT", exception.getMessage());
        verify(wasteBinRepository).findById("NONEXISTENT");
    }

    @Test
    void getBinsByResident_ShouldReturnBinsList_WhenResidentHasBins() {
        // Arrange
        List<WasteBin> mockBins = Arrays.asList(mockBin);
        when(wasteBinRepository.findByResidentId(1L)).thenReturn(mockBins);

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BIN-001", result.get(0).getBinId());
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsHigh() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 85.0);

        // Assert
        assertNotNull(result);
        assertEquals(85.0, result.getCurrentLevel());
        assertEquals(BinStatus.NEEDS_EMPTYING, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }
}
