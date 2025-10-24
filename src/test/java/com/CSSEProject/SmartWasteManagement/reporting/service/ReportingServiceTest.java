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
}
