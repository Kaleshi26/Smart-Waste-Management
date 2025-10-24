// Testing ReportingService analytics and dashboard functionality with mocked repositories
package com.CSSEProject.SmartWasteManagement.reporting.service;

import com.CSSEProject.SmartWasteManagement.dto.DashboardStatsDto;
import com.CSSEProject.SmartWasteManagement.reporting.service.ReportingService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportingService analytics and dashboard functionality.
 * Tests dashboard statistics and collection event reporting with mocked repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private CollectionEventRepository collectionEventRepository;

    @Mock
    private WasteBinRepository wasteBinRepository;

    @InjectMocks
    private ReportingService reportingService;

    private User mockStaff;
    private WasteBin mockBin;
    private CollectionEvent mockCollection1;
    private CollectionEvent mockCollection2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Arrange - Setup test data
        mockStaff = new User();
        mockStaff.setId(1L);
        mockStaff.setName("Staff Member");
        mockStaff.setRole(UserRole.ROLE_STAFF);

        mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");
        mockBin.setLocation("123 Main St");
        mockBin.setBinType(BinType.GENERAL_WASTE);
        mockBin.setCapacity(120.0);
        mockBin.setCurrentLevel(75.0);
        mockBin.setStatus(BinStatus.ACTIVE);

        mockCollection1 = new CollectionEvent();
        mockCollection1.setId(1L);
        mockCollection1.setCollectionTime(LocalDateTime.now().minusHours(2));
        mockCollection1.setWeight(15.5);
        mockCollection1.setCalculatedCharge(25.0);
        mockCollection1.setWasteBin(mockBin);
        mockCollection1.setCollector(mockStaff);

        mockCollection2 = new CollectionEvent();
        mockCollection2.setId(2L);
        mockCollection2.setCollectionTime(LocalDateTime.now().minusHours(1));
        mockCollection2.setWeight(22.3);
        mockCollection2.setCalculatedCharge(35.0);
        mockCollection2.setWasteBin(mockBin);
        mockCollection2.setCollector(mockStaff);
    }

    @Test
    void getDashboardStats_ShouldReturnCorrectStats_WhenDataExists() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalCollections());
        assertEquals(37.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3
        assertEquals(60.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0
        assertEquals(5, result.getTotalBins());
        assertEquals(4, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getCollectionEvents_ShouldReturnEvents_WhenEventsExist() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findAll()).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEvents();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(15.5, result.get(0).getWeight(), 0.001);
        assertEquals(22.3, result.get(1).getWeight(), 0.001);
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getCollectionEventsByCollector_ShouldReturnFilteredEvents_WhenCollectorHasEvents() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findByCollectorId(1L)).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByCollector(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(collectionEventRepository).findByCollectorId(1L);
    }

    @Test
    void getCollectionEventsByBin_ShouldReturnFilteredEvents_WhenBinHasEvents() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findByWasteBinBinId("BIN-001")).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByBin("BIN-001");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BIN-001", result.get(0).getWasteBin().getBinId());
        assertEquals("BIN-001", result.get(1).getWasteBin().getBinId());
        verify(collectionEventRepository).findByWasteBinBinId("BIN-001");
    }

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void getDashboardStats_ShouldReturnZeroStats_WhenNoDataExists() {
        // Arrange
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalCollections());
        assertEquals(0.0, result.getTotalWasteCollected(), 0.001);
        assertEquals(0.0, result.getTotalRevenue(), 0.001);
        assertEquals(0, result.getTotalBins());
        assertEquals(0, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getDashboardStats_ShouldHandleNullCollections_WhenRepositoryReturnsNull() {
        // Arrange
        when(collectionEventRepository.findAll()).thenReturn(null);
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalCollections());
        assertEquals(0.0, result.getTotalWasteCollected(), 0.001);
        assertEquals(0.0, result.getTotalRevenue(), 0.001);
        assertEquals(5, result.getTotalBins());
        assertEquals(4, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getDashboardStats_ShouldCalculateCorrectTotals_WhenMultipleCollectionsExist() {
        // Arrange
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(30.0);
        collection3.setCalculatedCharge(50.0);
        collection3.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, collection3);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(10L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(8L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalCollections());
        assertEquals(67.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + 30.0
        assertEquals(110.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + 50.0
        assertEquals(10, result.getTotalBins());
        assertEquals(8, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getCollectionEvents_ShouldReturnEmptyList_WhenNoEventsExist() {
        // Arrange
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEvents();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getCollectionEvents_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(collectionEventRepository.findAll()).thenReturn(null);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEvents();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getCollectionEventsByCollector_ShouldReturnEmptyList_WhenCollectorHasNoEvents() {
        // Arrange
        when(collectionEventRepository.findByCollectorId(999L)).thenReturn(Collections.emptyList());

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByCollector(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findByCollectorId(999L);
    }

    @Test
    void getCollectionEventsByCollector_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(collectionEventRepository.findByCollectorId(999L)).thenReturn(null);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByCollector(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findByCollectorId(999L);
    }

    @Test
    void getCollectionEventsByBin_ShouldReturnEmptyList_WhenBinHasNoEvents() {
        // Arrange
        when(collectionEventRepository.findByWasteBinBinId("NONEXISTENT")).thenReturn(Collections.emptyList());

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByBin("NONEXISTENT");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findByWasteBinBinId("NONEXISTENT");
    }

    @Test
    void getCollectionEventsByBin_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(collectionEventRepository.findByWasteBinBinId("NONEXISTENT")).thenReturn(null);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByBin("NONEXISTENT");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(collectionEventRepository).findByWasteBinBinId("NONEXISTENT");
    }

    @Test
    void getDashboardStats_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(collectionEventRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reportingService.getDashboardStats();
        });
        
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getCollectionEvents_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(collectionEventRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reportingService.getCollectionEvents();
        });
        
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getCollectionEventsByCollector_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(collectionEventRepository.findByCollectorId(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reportingService.getCollectionEventsByCollector(1L);
        });
        
        verify(collectionEventRepository).findByCollectorId(1L);
    }

    @Test
    void getCollectionEventsByBin_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(collectionEventRepository.findByWasteBinBinId("BIN-001")).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reportingService.getCollectionEventsByBin("BIN-001");
        });
        
        verify(collectionEventRepository).findByWasteBinBinId("BIN-001");
    }

    @Test
    void getDashboardStats_ShouldCalculateCorrectAverages_WhenMultipleCollectionsExist() {
        // Arrange
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(10.0);
        collection3.setCalculatedCharge(15.0);
        collection3.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        
        CollectionEvent collection4 = new CollectionEvent();
        collection4.setId(4L);
        collection4.setWeight(5.0);
        collection4.setCalculatedCharge(10.0);
        collection4.setCollectionTime(LocalDateTime.now().minusMinutes(45));
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, collection3, collection4);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(15L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(12L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(4, result.getTotalCollections());
        assertEquals(52.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + 10.0 + 5.0
        assertEquals(85.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + 15.0 + 10.0
        assertEquals(15, result.getTotalBins());
        assertEquals(12, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getDashboardStats_ShouldHandleZeroWeights_WhenCollectionsHaveZeroWeight() {
        // Arrange
        CollectionEvent zeroWeightCollection = new CollectionEvent();
        zeroWeightCollection.setId(3L);
        zeroWeightCollection.setWeight(0.0);
        zeroWeightCollection.setCalculatedCharge(0.0);
        zeroWeightCollection.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, zeroWeightCollection);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalCollections());
        assertEquals(37.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + 0.0
        assertEquals(60.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + 0.0
        assertEquals(5, result.getTotalBins());
        assertEquals(4, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getDashboardStats_ShouldHandleNegativeValues_WhenCollectionsHaveNegativeValues() {
        // Arrange
        CollectionEvent negativeWeightCollection = new CollectionEvent();
        negativeWeightCollection.setId(3L);
        negativeWeightCollection.setWeight(-5.0);
        negativeWeightCollection.setCalculatedCharge(-10.0);
        negativeWeightCollection.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, negativeWeightCollection);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalCollections());
        assertEquals(32.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + (-5.0)
        assertEquals(50.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + (-10.0)
        assertEquals(5, result.getTotalBins());
        assertEquals(4, result.getActiveBins());
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    void getCollectionEventsByCollector_ShouldReturnMultipleEvents_WhenCollectorHasMultipleEvents() {
        // Arrange
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(30.0);
        collection3.setCalculatedCharge(50.0);
        collection3.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        collection3.setCollector(mockStaff);
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, collection3);
        when(collectionEventRepository.findByCollectorId(1L)).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByCollector(1L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
        verify(collectionEventRepository).findByCollectorId(1L);
    }

    @Test
    void getCollectionEventsByBin_ShouldReturnMultipleEvents_WhenBinHasMultipleEvents() {
        // Arrange
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(30.0);
        collection3.setCalculatedCharge(50.0);
        collection3.setCollectionTime(LocalDateTime.now().minusMinutes(30));
        collection3.setWasteBin(mockBin);
        
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2, collection3);
        when(collectionEventRepository.findByWasteBinBinId("BIN-001")).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByBin("BIN-001");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("BIN-001", result.get(0).getWasteBin().getBinId());
        assertEquals("BIN-001", result.get(1).getWasteBin().getBinId());
        assertEquals("BIN-001", result.get(2).getWasteBin().getBinId());
        verify(collectionEventRepository).findByWasteBinBinId("BIN-001");
    }
}
