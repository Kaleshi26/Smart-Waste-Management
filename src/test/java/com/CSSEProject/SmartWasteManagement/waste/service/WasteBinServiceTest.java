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

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void getBinById_ShouldHandleInvalidBinId_WhenBinIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.getBinById(null);
        });
    }

    @Test
    void getBinById_ShouldHandleInvalidBinId_WhenBinIdIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.getBinById("");
        });
    }

    @Test
    void getBinById_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            wasteBinService.getBinById("BIN-001");
        });
        
        verify(wasteBinRepository).findById("BIN-001");
    }

    @Test
    void getBinsByResident_ShouldHandleInvalidResidentId_WhenResidentIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.getBinsByResident(null);
        });
    }

    @Test
    void getBinsByResident_ShouldHandleInvalidResidentId_WhenResidentIdIsZero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.getBinsByResident(0L);
        });
    }

    @Test
    void getBinsByResident_ShouldHandleInvalidResidentId_WhenResidentIdIsNegative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.getBinsByResident(-1L);
        });
    }

    @Test
    void getBinsByResident_ShouldReturnEmptyList_WhenResidentHasNoBins() {
        // Arrange
        when(wasteBinRepository.findByResidentId(999L)).thenReturn(Collections.emptyList());

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wasteBinRepository).findByResidentId(999L);
    }

    @Test
    void getBinsByResident_ShouldReturnMultipleBins_WhenResidentHasMultipleBins() {
        // Arrange
        WasteBin bin2 = new WasteBin();
        bin2.setBinId("BIN-002");
        bin2.setLocation("456 Oak St");
        bin2.setBinType(BinType.RECYCLABLE);
        bin2.setCapacity(100.0);
        bin2.setCurrentLevel(30.0);
        bin2.setStatus(BinStatus.ACTIVE);
        bin2.setResident(mockResident);

        List<WasteBin> mockBins = Arrays.asList(mockBin, bin2);
        when(wasteBinRepository.findByResidentId(1L)).thenReturn(mockBins);

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BIN-001", result.get(0).getBinId());
        assertEquals("BIN-002", result.get(1).getBinId());
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    void getBinsByResident_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(wasteBinRepository.findByResidentId(1L)).thenReturn(null);

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    void getBinsByResident_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(wasteBinRepository.findByResidentId(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            wasteBinService.getBinsByResident(1L);
        });
        
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    void updateBinLevel_ShouldHandleInvalidBinId_WhenBinIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.updateBinLevel(null, 50.0);
        });
    }

    @Test
    void updateBinLevel_ShouldHandleInvalidBinId_WhenBinIdIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.updateBinLevel("", 50.0);
        });
    }

    @Test
    void updateBinLevel_ShouldHandleInvalidLevel_WhenLevelIsNegative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.updateBinLevel("BIN-001", -10.0);
        });
    }

    @Test
    void updateBinLevel_ShouldHandleInvalidLevel_WhenLevelIsTooLarge() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wasteBinService.updateBinLevel("BIN-001", 150.0);
        });
    }

    @Test
    void updateBinLevel_ShouldHandleNonExistentBin_WhenBinNotFound() {
        // Arrange
        when(wasteBinRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wasteBinService.updateBinLevel("NONEXISTENT", 50.0);
        });

        assertEquals("Bin not found with ID: NONEXISTENT", exception.getMessage());
        verify(wasteBinRepository).findById("NONEXISTENT");
    }

    @Test
    void updateBinLevel_ShouldHandleRepositoryExceptions_WhenRepositoryFails() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            wasteBinService.updateBinLevel("BIN-001", 50.0);
        });
        
        verify(wasteBinRepository).findById("BIN-001");
    }

    @Test
    void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsLow() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 30.0);

        // Assert
        assertNotNull(result);
        assertEquals(30.0, result.getCurrentLevel());
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsMedium() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 60.0);

        // Assert
        assertNotNull(result);
        assertEquals(60.0, result.getCurrentLevel());
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsFull() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 100.0);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getCurrentLevel());
        assertEquals(BinStatus.FULL, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsCritical() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 95.0);

        // Assert
        assertNotNull(result);
        assertEquals(95.0, result.getCurrentLevel());
        assertEquals(BinStatus.NEEDS_EMPTYING, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldHandleSaveExceptions_WhenSaveFails() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenThrow(new RuntimeException("Database save failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            wasteBinService.updateBinLevel("BIN-001", 50.0);
        });
        
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void getBinById_ShouldReturnBinWithCorrectDetails_WhenBinExists() {
        // Arrange
        mockBin.setBinType(BinType.RECYCLABLE);
        mockBin.setCapacity(150.0);
        mockBin.setCurrentLevel(60.0);
        mockBin.setStatus(BinStatus.ACTIVE);
        mockBin.setInstallationDate(LocalDate.now().minusDays(30));
        
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));

        // Act
        WasteBin result = wasteBinService.getBinById("BIN-001");

        // Assert
        assertNotNull(result);
        assertEquals("BIN-001", result.getBinId());
        assertEquals("123 Main St", result.getLocation());
        assertEquals(BinType.RECYCLABLE, result.getBinType());
        assertEquals(150.0, result.getCapacity(), 0.001);
        assertEquals(60.0, result.getCurrentLevel(), 0.001);
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        assertEquals(mockResident, result.getResident());
        verify(wasteBinRepository).findById("BIN-001");
    }

    @Test
    void getBinsByResident_ShouldReturnBinsWithDifferentTypes_WhenResidentHasMixedBins() {
        // Arrange
        WasteBin recyclableBin = new WasteBin();
        recyclableBin.setBinId("BIN-002");
        recyclableBin.setLocation("456 Oak St");
        recyclableBin.setBinType(BinType.RECYCLABLE);
        recyclableBin.setCapacity(100.0);
        recyclableBin.setCurrentLevel(30.0);
        recyclableBin.setStatus(BinStatus.ACTIVE);
        recyclableBin.setResident(mockResident);

        WasteBin organicBin = new WasteBin();
        organicBin.setBinId("BIN-003");
        organicBin.setLocation("789 Pine St");
        organicBin.setBinType(BinType.ORGANIC);
        organicBin.setCapacity(80.0);
        organicBin.setCurrentLevel(70.0);
        organicBin.setStatus(BinStatus.NEEDS_EMPTYING);
        organicBin.setResident(mockResident);

        List<WasteBin> mockBins = Arrays.asList(mockBin, recyclableBin, organicBin);
        when(wasteBinRepository.findByResidentId(1L)).thenReturn(mockBins);

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(BinType.GENERAL_WASTE, result.get(0).getBinType());
        assertEquals(BinType.RECYCLABLE, result.get(1).getBinType());
        assertEquals(BinType.ORGANIC, result.get(2).getBinType());
        assertEquals(BinStatus.ACTIVE, result.get(0).getStatus());
        assertEquals(BinStatus.ACTIVE, result.get(1).getStatus());
        assertEquals(BinStatus.NEEDS_EMPTYING, result.get(2).getStatus());
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    void updateBinLevel_ShouldHandleBoundaryValues_WhenLevelIsExactlyZero() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 0.0);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getCurrentLevel());
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldHandleBoundaryValues_WhenLevelIsExactly100() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 100.0);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getCurrentLevel());
        assertEquals(BinStatus.FULL, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldHandleBoundaryValues_WhenLevelIsExactly80() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 80.0);

        // Assert
        assertNotNull(result);
        assertEquals(80.0, result.getCurrentLevel());
        assertEquals(BinStatus.NEEDS_EMPTYING, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldHandleBoundaryValues_WhenLevelIsExactly79() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 79.0);

        // Assert
        assertNotNull(result);
        assertEquals(79.0, result.getCurrentLevel());
        assertEquals(BinStatus.ACTIVE, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }

    @Test
    void updateBinLevel_ShouldHandleBoundaryValues_WhenLevelIsExactly81() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 81.0);

        // Assert
        assertNotNull(result);
        assertEquals(81.0, result.getCurrentLevel());
        assertEquals(BinStatus.NEEDS_EMPTYING, result.getStatus());
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }
}
