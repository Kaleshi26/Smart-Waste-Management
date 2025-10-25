package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclableItemDto;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingType;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.entity.*;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionScheduleRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionEventRepository collectionRepository;

    @Mock
    private WasteBinRepository wasteBinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BillingService billingService;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private CollectionScheduleRepository collectionScheduleRepository;

    @Mock
    private RecyclingCollectionRepository recyclingCollectionRepository;

    @InjectMocks
    private CollectionService collectionService;

    private WasteBin mockBin;
    private User mockResident;
    private User mockCollector;
    private BillingModel mockBillingModel;
    private CollectionRequestDto validRequest;

    @BeforeEach
    void setUp() {
        // Setup mock resident
        mockResident = new User();
        mockResident.setId(1L);
        mockResident.setName("John Resident");
        mockResident.setAddress("123 Main Street, Colombo");
        mockResident.setPendingCharges(0.0);
        mockResident.setRecyclingCredits(0.0);

        // Setup mock collector
        mockCollector = new User();
        mockCollector.setId(100L);
        mockCollector.setName("Staff Collector");

        // Setup mock bin
        mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");
        mockBin.setLocation("123 Main Street");
        mockBin.setBinType(BinType.GENERAL_WASTE);
        mockBin.setCapacity(120.0);
        mockBin.setCurrentLevel(75.0);
        mockBin.setResident(mockResident);

        // Setup mock billing model
        mockBillingModel = new BillingModel();
        mockBillingModel.setBillingType(BillingType.WEIGHT_BASED);
        mockBillingModel.setRatePerKg(5.0);
        mockBillingModel.setCity("Colombo");

        // Setup valid collection request
        validRequest = new CollectionRequestDto();
        validRequest.setBinId("BIN-001");
        validRequest.setCollectorId(100L);
        validRequest.setWeight(10.5);
        validRequest.setTruckId("TRUCK-001");
        // Remove notes since setNotes method doesn't exist
    }

    @Test
    void recordCollection_WithValidRequest_ShouldSaveCollection() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockCollector));
        when(billingService.getActiveBillingModelForCity("Colombo")).thenReturn(mockBillingModel);

        CollectionSchedule mockSchedule = new CollectionSchedule();
        mockSchedule.setStatus(ScheduleStatus.PENDING);
        when(collectionScheduleRepository.findPendingScheduleForBin(anyString(), any()))
                .thenReturn(Optional.of(mockSchedule));

        when(collectionRepository.save(any(CollectionEvent.class))).thenAnswer(invocation -> {
            CollectionEvent collection = invocation.getArgument(0);
            collection.setId(1L);
            collection.setCalculatedCharge(52.5); // 10.5 * 5.0
            return collection;
        });

        // Act
        CollectionEvent result = collectionService.recordCollection(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("BIN-001", result.getWasteBin().getBinId());
        assertEquals(10.5, result.getWeight());
        assertEquals(52.5, result.getCalculatedCharge());

        verify(collectionRepository, atLeastOnce()).save(any(CollectionEvent.class));
        verify(wasteBinRepository).save(mockBin);
        verify(feedbackService).provideSuccessFeedback(anyString());
    }

    @Test
    void recordCollection_WithRecyclables_ShouldCalculateRefunds() {
        // Arrange
        RecyclableItemDto recyclable = new RecyclableItemDto();
        recyclable.setType(RecyclableType.PLASTIC);
        recyclable.setWeightKg(2.0);
        recyclable.setQuality(QualityGrade.GOOD);
        // Remove notes since it might not exist

        validRequest.setRecyclables(Arrays.asList(recyclable));

        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockCollector));
        when(billingService.getActiveBillingModelForCity("Colombo")).thenReturn(mockBillingModel);

        CollectionSchedule mockSchedule = new CollectionSchedule();
        when(collectionScheduleRepository.findPendingScheduleForBin(anyString(), any()))
                .thenReturn(Optional.of(mockSchedule));

        when(collectionRepository.save(any(CollectionEvent.class))).thenAnswer(invocation -> {
            CollectionEvent collection = invocation.getArgument(0);
            collection.setId(1L);
            collection.setCalculatedCharge(52.5);
            collection.setRecyclableWeight(2.0);
            collection.setRefundAmount(1.2);
            collection.setRecyclableItemsCount(1);
            return collection;
        });

        // Act
        CollectionEvent result = collectionService.recordCollection(validRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRecyclableItemsCount() > 0);
        assertTrue(result.getRefundAmount() > 0);
        assertEquals(2.0, result.getRecyclableWeight());
    }

    @Test
    void recordCollection_WithNonExistentBin_ShouldThrowException() {
        // Arrange
        when(wasteBinRepository.findById("INVALID-BIN")).thenReturn(Optional.empty());
        validRequest.setBinId("INVALID-BIN");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            collectionService.recordCollection(validRequest);
        });

        assertEquals("Bin not found: INVALID-BIN", exception.getMessage());
        verify(feedbackService).provideErrorFeedback("Bin not found: INVALID-BIN");
    }

    @Test
    void recordCollection_WithBinWithoutResident_ShouldThrowException() {
        // Arrange
        mockBin.setResident(null);
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            collectionService.recordCollection(validRequest);
        });

        assertEquals("Bin not assigned to any resident: BIN-001", exception.getMessage());
        verify(feedbackService).provideErrorFeedback("Bin not assigned to any resident");
    }

    @Test
    void calculateCollectionCharge_WeightBasedBilling_ShouldCalculateCorrectly() {
        // Arrange
        mockBillingModel.setBillingType(BillingType.WEIGHT_BASED);
        mockBillingModel.setRatePerKg(5.0);

        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockCollector));
        when(billingService.getActiveBillingModelForCity("Colombo")).thenReturn(mockBillingModel);

        CollectionSchedule mockSchedule = new CollectionSchedule();
        when(collectionScheduleRepository.findPendingScheduleForBin(anyString(), any()))
                .thenReturn(Optional.of(mockSchedule));

        when(collectionRepository.save(any(CollectionEvent.class))).thenAnswer(invocation -> {
            CollectionEvent collection = invocation.getArgument(0);
            collection.setId(1L);
            collection.setCalculatedCharge(50.0); // 10.0 * 5.0
            return collection;
        });

        validRequest.setWeight(10.0);
        CollectionEvent result = collectionService.recordCollection(validRequest);

        // Assert
        assertEquals(50.0, result.getCalculatedCharge());
    }

    @Test
    void getCollectionsByCollector_ShouldReturnCollections() {
        // Arrange
        CollectionEvent collection1 = new CollectionEvent();
        collection1.setId(1L);
        collection1.setWeight(10.0);
        collection1.setWasteBin(mockBin);

        CollectionEvent collection2 = new CollectionEvent();
        collection2.setId(2L);
        collection2.setWeight(15.0);
        collection2.setWasteBin(mockBin);

        List<CollectionEvent> mockCollections = Arrays.asList(collection1, collection2);
        when(collectionRepository.findByCollectorId(100L)).thenReturn(mockCollections);

        // Act
        List<CollectionEvent> result = collectionService.getCollectionsByCollector(100L);

        // Assert
        assertEquals(2, result.size());
        verify(collectionRepository).findByCollectorId(100L);
    }

    @Test
    void updateBinLevel_WithValidLevel_ShouldUpdateBin() {
        // Arrange
        when(wasteBinRepository.findById("BIN-001")).thenReturn(Optional.of(mockBin));
        when(wasteBinRepository.save(any(WasteBin.class))).thenReturn(mockBin);

        // Act
        WasteBin result = collectionService.updateBinLevel("BIN-001", 85.0);

        // Assert
        assertNotNull(result);
        assertEquals(85.0, result.getCurrentLevel());
        verify(wasteBinRepository).save(mockBin);
    }

    @Test
    void updateBinLevel_WithInvalidLevel_ShouldThrowException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            collectionService.updateBinLevel("BIN-001", 150.0);
        });

        assertEquals("Bin level must be between 0 and 100", exception.getMessage());
    }

    @Test
    void isBinScheduledForCollectionToday_WithSchedule_ShouldReturnTrue() {
        // Arrange
        when(collectionScheduleRepository.findPendingScheduleForBin("BIN-001", java.time.LocalDate.now()))
                .thenReturn(Optional.of(new CollectionSchedule()));

        // Act
        boolean result = collectionService.isBinScheduledForCollectionToday("BIN-001");

        // Assert
        assertTrue(result);
    }

    @Test
    void getResidentRecyclingCredits_ShouldReturnCredits() {
        // Arrange
        mockResident.setRecyclingCredits(150.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockResident));

        // Act
        Double result = collectionService.getResidentRecyclingCredits(1L);

        // Assert
        assertEquals(150.0, result);
    }

    @Test
    void getTotalWasteCollectedBetween_ShouldReturnTotalWeight() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(collectionRepository.getTotalWeightBetween(start, end)).thenReturn(500.0);

        // Act
        Double result = collectionService.getTotalWasteCollectedBetween(start, end);

        // Assert
        assertEquals(500.0, result);
    }

    @Test
    void getCollectionCountBetween_ShouldReturnCount() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(collectionRepository.getCollectionCountBetween(start, end)).thenReturn(25L);

        // Act
        Long result = collectionService.getCollectionCountBetween(start, end);

        // Assert
        assertEquals(25L, result);
    }

    @Test
    void getUninvoicedCollections_ShouldReturnCollectionsWithoutInvoice() {
        // Arrange
        CollectionEvent uninvoicedCollection = new CollectionEvent();
        uninvoicedCollection.setId(1L);
        when(collectionRepository.findUninvoicedCollections()).thenReturn(Arrays.asList(uninvoicedCollection));

        // Act
        List<CollectionEvent> result = collectionService.getUninvoicedCollections();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getCollectionsByBin_ShouldReturnCollections() {
        // Arrange
        CollectionEvent collection = new CollectionEvent();
        collection.setId(1L);
        when(collectionRepository.findByWasteBinBinId("BIN-001")).thenReturn(Arrays.asList(collection));

        // Act
        List<CollectionEvent> result = collectionService.getCollectionsByBin("BIN-001");

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}