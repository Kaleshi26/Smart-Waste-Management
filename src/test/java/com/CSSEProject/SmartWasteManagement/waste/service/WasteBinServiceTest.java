package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WasteBinServiceTest {

    @Mock
    private WasteBinRepository wasteBinRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WasteBinService wasteBinService;

    private WasteBin mockBin;
    private User mockResident;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

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

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void getBinById_ShouldReturnBin_WhenBinExists() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));

        // Act
        WasteBin result = wasteBinService.getBinById("BIN-001");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getBinId(), "BIN-001");
        Assert.assertEquals(result.getLocation(), "123 Main St");
        Assert.assertEquals(result.getBinType(), BinType.GENERAL_WASTE);
        Assert.assertEquals(result.getStatus(), BinStatus.ACTIVE);
        verify(wasteBinRepository).findById("BIN-001");
    }

    @Test
    public void getBinById_ShouldThrowException_WhenBinNotFound() {
        // Arrange
        when(wasteBinRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = Assert.expectThrows(RuntimeException.class, () -> {
            wasteBinService.getBinById("NONEXISTENT");
        });

        Assert.assertEquals(exception.getMessage(), "Bin not found with ID: NONEXISTENT");
        verify(wasteBinRepository).findById("NONEXISTENT");
    }

    @Test
    public void getBinsByResident_ShouldReturnBinsList_WhenResidentHasBins() {
        // Arrange
        List<WasteBin> mockBins = Arrays.asList(mockBin);
        when(wasteBinRepository.findByResidentId(1L)).thenReturn(mockBins);

        // Act
        List<WasteBin> result = wasteBinService.getBinsByResident(1L);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getBinId(), "BIN-001");
        verify(wasteBinRepository).findByResidentId(1L);
    }

    @Test
    public void updateBinLevel_ShouldUpdateLevelAndStatus_WhenLevelIsHigh() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = wasteBinService.updateBinLevel("BIN-001", 85.0);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getCurrentLevel(), 85.0);
        Assert.assertEquals(result.getStatus(), BinStatus.NEEDS_EMPTYING);
        verify(wasteBinRepository).findById("BIN-001");
        verify(wasteBinRepository).save(any(WasteBin.class));
    }
}