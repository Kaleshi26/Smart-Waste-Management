// Testing AnalyticsService business logic with mocked repositories
package com.CSSEProject.SmartWasteManagement.analytics.service;

import com.CSSEProject.SmartWasteManagement.analytics.dto.*;
import com.CSSEProject.SmartWasteManagement.analytics.strategy.FilterStrategy;
import com.CSSEProject.SmartWasteManagement.analytics.strategy.FilterStrategyFactory;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService business logic.
 * Tests analytics calculations and data processing with mocked repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CollectionEventRepository collectionEventRepository;

    @Mock
    private WasteBinRepository wasteBinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private FilterStrategyFactory filterStrategyFactory;

    @Mock
    private FilterStrategy filterStrategy;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User mockStaff;
    private User mockResident;
    private WasteBin mockBin;
    private CollectionEvent mockCollection1;
    private CollectionEvent mockCollection2;
    private Invoice mockInvoice;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        mockStaff = new User();
        mockStaff.setId(1L);
        mockStaff.setName("Staff Member");
        mockStaff.setRole(UserRole.ROLE_STAFF);

        mockResident = new User();
        mockResident.setId(2L);
        mockResident.setName("Resident User");
        mockResident.setRole(UserRole.ROLE_RESIDENT);

        mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");
        mockBin.setLocation("123 Main St");
        mockBin.setBinType(BinType.GENERAL_WASTE);
        mockBin.setCapacity(120.0);
        mockBin.setCurrentLevel(75.0);
        mockBin.setStatus(BinStatus.ACTIVE);
        mockBin.setResident(mockResident);

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

        mockInvoice = new Invoice();
        mockInvoice.setId(1L);
        mockInvoice.setInvoiceNumber("INV-001");
        mockInvoice.setStatus(InvoiceStatus.PAID);
        mockInvoice.setTotalAmount(60.0);
        mockInvoice.setResident(mockResident);
    }

    @Test
    void getAnalyticsData_ShouldReturnValidKPIs_WhenDataExists() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);
        when(userRepository.count()).thenReturn(10L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        AnalyticsDataDto result = analyticsService.getAnalyticsData("30");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getKpis());
        assertEquals(2, result.getKpis().getTotalCollections());
        assertEquals(37.8, result.getKpis().getTotalWasteCollected(), 0.001);
        assertEquals(60.0, result.getKpis().getTotalRevenue(), 0.001);
        assertEquals(5, result.getKpis().getTotalBins());
        assertEquals(4, result.getKpis().getActiveBins());
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void getKPIs_ShouldReturnCorrectMetrics_WhenRangeIs7Days() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("7")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(3L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.count()).thenReturn(5L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        KPIsDto result = analyticsService.getKPIs("7");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalCollections());
        assertEquals(37.8, result.getTotalWasteCollected(), 0.001);
        assertEquals(60.0, result.getTotalRevenue(), 0.001);
        assertEquals(3, result.getTotalBins());
        assertEquals(2, result.getActiveBins());
        verify(filterStrategyFactory).getStrategy("7");
    }

    @Test
    void getMonthlyData_ShouldReturnMonthlyStatistics_WhenDataExists() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));

        // Act
        List<MonthlyDataDto> result = analyticsService.getMonthlyData("30");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void getCollectionRecords_ShouldReturnFilteredRecords_WhenDateRangeProvided() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("7")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));

        // Act
        List<CollectionRecordDto> result = analyticsService.getCollectionRecords("7");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(filterStrategyFactory).getStrategy("7");
    }

    @Test
    void getBinStatusOverview_ShouldReturnBinStatusData_WhenBinsExist() {
        // Arrange
        when(wasteBinRepository.findAll()).thenReturn(Arrays.asList(mockBin));

        // Act
        List<BinStatusDto> result = analyticsService.getBinStatusOverview();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(wasteBinRepository).findAll();
    }

    @Test
    void exportToCSV_ShouldGenerateCSVData_WhenRequested() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);
        when(userRepository.count()).thenReturn(10L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        String result = analyticsService.exportToCSV("30");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Collection ID"));
        assertTrue(result.contains("Weight"));
        assertTrue(result.contains("Charge"));
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void getAnalyticsSummary_ShouldReturnSummaryData_WhenSystemHasData() {
        // Arrange
        when(collectionEventRepository.count()).thenReturn(100L);
        when(wasteBinRepository.count()).thenReturn(25L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(20L);
        when(wasteBinRepository.countByStatus(BinStatus.NEEDS_EMPTYING)).thenReturn(5L);
        when(userRepository.count()).thenReturn(50L);
        when(invoiceRepository.count()).thenReturn(75L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("totalCollections"));
        assertTrue(result.containsKey("totalBins"));
        assertTrue(result.containsKey("activeBins"));
        assertTrue(result.containsKey("totalUsers"));
        assertTrue(result.containsKey("totalInvoices"));
        verify(collectionEventRepository).count();
        verify(wasteBinRepository).count();
    }

    @Test
    void getAnalyticsData_ShouldHandleEmptyData_WhenNoCollectionsExist() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Collections.emptyList());

        // Act
        AnalyticsDataDto result = analyticsService.getAnalyticsData("30");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getKpis());
        assertEquals(0, result.getKpis().getTotalCollections());
        assertEquals(0.0, result.getKpis().getTotalWasteCollected(), 0.001);
        assertEquals(0.0, result.getKpis().getTotalRevenue(), 0.001);
        assertEquals(0, result.getKpis().getTotalBins());
        assertEquals(0, result.getKpis().getActiveBins());
    }

    @Test
    void getAnalyticsData_ShouldHandleInvalidRange_WhenInvalidRangeProvided() {
        // Arrange
        when(filterStrategyFactory.getStrategy("invalid")).thenThrow(new IllegalArgumentException("Invalid range"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.getAnalyticsData("invalid");
        });
        
        verify(filterStrategyFactory).getStrategy("invalid");
    }

    @Test
    void calculateKPIs_ShouldCalculateCorrectTotals_WhenMultipleCollectionsExist() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Create additional collections for testing
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(30.0);
        collection3.setCalculatedCharge(50.0);
        collection3.setCollectionTime(LocalDateTime.now().minusHours(3));
        
        List<CollectionEvent> allCollections = Arrays.asList(mockCollection1, mockCollection2, collection3);
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(allCollections);
        when(wasteBinRepository.count()).thenReturn(10L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(8L);
        when(userRepository.count()).thenReturn(20L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        KPIsDto result = analyticsService.getKPIs("30");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalCollections());
        assertEquals(67.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + 30.0
        assertEquals(110.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + 50.0
        assertEquals(10, result.getTotalBins());
        assertEquals(8, result.getActiveBins());
    }

    @Test
    void getAnalyticsData_ShouldHandleNullValues_WhenRepositoryReturnsNull() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(null);
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(null);

        // Act
        AnalyticsDataDto result = analyticsService.getAnalyticsData("30");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getKpis());
        assertEquals(0, result.getKpis().getTotalCollections());
        assertEquals(0.0, result.getKpis().getTotalWasteCollected(), 0.001);
        assertEquals(0.0, result.getKpis().getTotalRevenue(), 0.001);
    }

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void getAnalyticsData_ShouldReturnValidData_WhenRangeIsAll() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("all")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(15L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(12L);
        when(userRepository.count()).thenReturn(30L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        AnalyticsDataDto result = analyticsService.getAnalyticsData("all");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getKpis());
        assertEquals(2, result.getKpis().getTotalCollections());
        assertEquals(15, result.getKpis().getTotalBins());
        assertEquals(12, result.getKpis().getActiveBins());
        verify(filterStrategyFactory).getStrategy("all");
    }

    @Test
    void getMonthlyData_ShouldReturnEmptyList_WhenNoCollectionsInRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("7")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<MonthlyDataDto> result = analyticsService.getMonthlyData("7");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(filterStrategyFactory).getStrategy("7");
    }

    @Test
    void getCollectionRecords_ShouldReturnEmptyList_WhenNoCollectionsExist() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("7")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CollectionRecordDto> result = analyticsService.getCollectionRecords("7");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(filterStrategyFactory).getStrategy("7");
    }

    @Test
    void getBinStatusOverview_ShouldReturnEmptyList_WhenNoBinsExist() {
        // Arrange
        when(wasteBinRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<BinStatusDto> result = analyticsService.getBinStatusOverview();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wasteBinRepository).findAll();
    }

    @Test
    void exportToCSV_ShouldHandleEmptyData_WhenNoCollectionsExist() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Collections.emptyList());
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Collections.emptyList());

        // Act
        String result = analyticsService.exportToCSV("30");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Collection ID"));
        assertTrue(result.contains("Weight"));
        assertTrue(result.contains("Charge"));
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void getAnalyticsSummary_ShouldHandleZeroCounts_WhenNoDataExists() {
        // Arrange
        when(collectionEventRepository.count()).thenReturn(0L);
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.NEEDS_EMPTYING)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(invoiceRepository.count()).thenReturn(0L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.get("totalCollections"));
        assertEquals(0L, result.get("totalBins"));
        assertEquals(0L, result.get("activeBins"));
        assertEquals(0L, result.get("totalUsers"));
        assertEquals(0L, result.get("totalInvoices"));
        verify(collectionEventRepository).count();
        verify(wasteBinRepository).count();
    }

    @Test
    void getKPIs_ShouldCalculateCorrectAverages_WhenMultipleCollectionsExist() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Create multiple collections with different weights
        CollectionEvent collection3 = new CollectionEvent();
        collection3.setId(3L);
        collection3.setWeight(10.0);
        collection3.setCalculatedCharge(20.0);
        collection3.setCollectionTime(LocalDateTime.now().minusHours(3));
        
        CollectionEvent collection4 = new CollectionEvent();
        collection4.setId(4L);
        collection4.setWeight(5.0);
        collection4.setCalculatedCharge(10.0);
        collection4.setCollectionTime(LocalDateTime.now().minusHours(4));
        
        List<CollectionEvent> allCollections = Arrays.asList(mockCollection1, mockCollection2, collection3, collection4);
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(allCollections);
        when(wasteBinRepository.count()).thenReturn(10L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(8L);
        when(userRepository.count()).thenReturn(20L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        KPIsDto result = analyticsService.getKPIs("30");

        // Assert
        assertNotNull(result);
        assertEquals(4, result.getTotalCollections());
        assertEquals(52.8, result.getTotalWasteCollected(), 0.001); // 15.5 + 22.3 + 10.0 + 5.0
        assertEquals(90.0, result.getTotalRevenue(), 0.001); // 25.0 + 35.0 + 20.0 + 10.0
        assertEquals(10, result.getTotalBins());
        assertEquals(8, result.getActiveBins());
    }

    @Test
    void getAnalyticsData_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(collectionEventRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            analyticsService.getAnalyticsData("30");
        });
        
        verify(filterStrategyFactory).getStrategy("30");
        verify(collectionEventRepository).findAll();
    }

    @Test
    void getKPIs_ShouldHandleNullFilterStrategy_WhenStrategyFactoryReturnsNull() {
        // Arrange
        when(filterStrategyFactory.getStrategy("30")).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            analyticsService.getKPIs("30");
        });
        
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void exportToCSV_ShouldGenerateValidCSVFormat_WhenDataExists() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);
        when(userRepository.count()).thenReturn(10L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        String result = analyticsService.exportToCSV("30");

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("Collection ID"));
        assertTrue(result.contains(","));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("15.5"));
        assertTrue(result.contains("25.0"));
        verify(filterStrategyFactory).getStrategy("30");
    }

    @Test
    void getAnalyticsData_ShouldReturnConsistentData_WhenCalledMultipleTimes() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(filterStrategyFactory.getStrategy("30")).thenReturn(filterStrategy);
        when(filterStrategy.getStartDate()).thenReturn(startDate);
        when(filterStrategy.getEndDate()).thenReturn(endDate);
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList(mockCollection1, mockCollection2));
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);
        when(userRepository.count()).thenReturn(10L);
        when(invoiceRepository.findByStatus(InvoiceStatus.PAID)).thenReturn(Arrays.asList(mockInvoice));

        // Act
        AnalyticsDataDto result1 = analyticsService.getAnalyticsData("30");
        AnalyticsDataDto result2 = analyticsService.getAnalyticsData("30");

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getKpis().getTotalCollections(), result2.getKpis().getTotalCollections());
        assertEquals(result1.getKpis().getTotalWasteCollected(), result2.getKpis().getTotalWasteCollected());
        assertEquals(result1.getKpis().getTotalRevenue(), result2.getKpis().getTotalRevenue());
        verify(filterStrategyFactory, times(2)).getStrategy("30");
    }
}
