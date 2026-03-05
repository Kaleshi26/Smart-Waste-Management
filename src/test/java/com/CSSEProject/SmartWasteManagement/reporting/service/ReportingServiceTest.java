package com.CSSEProject.SmartWasteManagement.reporting.service;

import com.CSSEProject.SmartWasteManagement.dto.DashboardStatsDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReportingServiceTest {

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
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

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

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void getDashboardStats_ShouldReturnCorrectStats_WhenDataExists() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findAll()).thenReturn(collections);
        when(wasteBinRepository.count()).thenReturn(5L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(4L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getTotalCollections(), 2);
        Assert.assertEquals(result.getTotalWasteCollected(), 37.8, 0.001); // 15.5 + 22.3
        Assert.assertEquals(result.getTotalRevenue(), 60.0, 0.001); // 25.0 + 35.0
        Assert.assertEquals(result.getTotalBins(), 5);
        Assert.assertEquals(result.getActiveBins(), 4);
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }

    @Test
    public void getCollectionEvents_ShouldReturnEvents_WhenEventsExist() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findAll()).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEvents();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getId(), 1L);
        Assert.assertEquals(result.get(1).getId(), 2L);
        Assert.assertEquals(result.get(0).getWeight(), 15.5, 0.001);
        Assert.assertEquals(result.get(1).getWeight(), 22.3, 0.001);
        verify(collectionEventRepository).findAll();
    }

    @Test
    public void getCollectionEventsByCollector_ShouldReturnFilteredEvents_WhenCollectorHasEvents() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findByCollectorId(1L)).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByCollector(1L);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getId(), 1L);
        Assert.assertEquals(result.get(1).getId(), 2L);
        verify(collectionEventRepository).findByCollectorId(1L);
    }

    @Test
    public void getCollectionEventsByBin_ShouldReturnFilteredEvents_WhenBinHasEvents() {
        // Arrange
        List<CollectionEvent> collections = Arrays.asList(mockCollection1, mockCollection2);
        when(collectionEventRepository.findByWasteBinBinId("BIN-001")).thenReturn(collections);

        // Act
        List<CollectionEvent> result = reportingService.getCollectionEventsByBin("BIN-001");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getWasteBin().getBinId(), "BIN-001");
        Assert.assertEquals(result.get(1).getWasteBin().getBinId(), "BIN-001");
        verify(collectionEventRepository).findByWasteBinBinId("BIN-001");
    }

    @Test
    public void getDashboardStats_ShouldHandleZeroData_WhenNoCollectionsOrBinsExist() {
        // Arrange - Empty database scenario
        when(collectionEventRepository.findAll()).thenReturn(Arrays.asList());
        when(wasteBinRepository.count()).thenReturn(0L);
        when(wasteBinRepository.countByStatus(BinStatus.ACTIVE)).thenReturn(0L);

        // Act
        DashboardStatsDto result = reportingService.getDashboardStats();

        // Assert - Should return zero values instead of null or errors
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getTotalCollections(), 0);
        Assert.assertEquals(result.getTotalWasteCollected(), 0.0, 0.001);
        Assert.assertEquals(result.getTotalRevenue(), 0.0, 0.001);
        Assert.assertEquals(result.getTotalBins(), 0);
        Assert.assertEquals(result.getActiveBins(), 0);
        verify(collectionEventRepository).findAll();
        verify(wasteBinRepository).count();
        verify(wasteBinRepository).countByStatus(BinStatus.ACTIVE);
    }
}