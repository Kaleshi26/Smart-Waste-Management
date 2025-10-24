package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.CollectionResponseDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclableItemDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import com.CSSEProject.SmartWasteManagement.waste.service.OfflineSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CollectionService collectionService;

    @Mock
    private OfflineSyncService offlineSyncService;

    @InjectMocks
    private CollectionController collectionController;

    private ObjectMapper objectMapper;
    private CollectionRequestDto validRequest;
    private CollectionEvent mockCollection;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(collectionController).build();
        objectMapper = new ObjectMapper();

        // Setup valid request
        validRequest = new CollectionRequestDto();
        validRequest.setBinId("BIN-001");
        validRequest.setCollectorId(100L);
        validRequest.setWeight(10.5);
        validRequest.setTruckId("TRUCK-001");

        // Setup mock collection
        mockCollection = new CollectionEvent();
        mockCollection.setId(1L);
        mockCollection.setWeight(10.5);

        WasteBin mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");
        mockBin.setLocation("123 Main Street");
        mockCollection.setWasteBin(mockBin);
    }

    @Test
    void recordCollection_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenReturn(mockCollection);

        // Act & Assert
        mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Collection recorded successfully"))
                .andExpect(jsonPath("$.collection.id").value(1L))
                .andExpect(jsonPath("$.feedback.audio").exists())
                .andExpect(jsonPath("$.feedback.visual").exists());

        verify(collectionService).recordCollection(any(CollectionRequestDto.class));
    }

    @Test
    void recordCollection_WithRecyclables_ShouldIncludeRecyclingSummary() throws Exception {
        // Arrange
        RecyclableItemDto recyclable = new RecyclableItemDto();
        recyclable.setType(com.CSSEProject.SmartWasteManagement.waste.entity.RecyclableType.PLASTIC);
        recyclable.setWeightKg(2.0);
        validRequest.setRecyclables(Arrays.asList(recyclable));

        mockCollection.setRecyclableWeight(2.0);
        mockCollection.setRefundAmount(1.2);
        mockCollection.setRecyclableItemsCount(1);

        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenReturn(mockCollection);

        // Act & Assert
        mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recyclingSummary.itemsCount").value(1))
                .andExpect(jsonPath("$.recyclingSummary.totalWeight").value(2.0))
                .andExpect(jsonPath("$.recyclingSummary.totalRefund").value(1.2));
    }

    @Test
    void recordCollection_WithServiceError_ShouldReturnError() throws Exception {
        // Arrange
        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenThrow(new RuntimeException("Bin not found"));

        // Act & Assert
        mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bin not found"))
                .andExpect(jsonPath("$.feedback.audio").exists())
                .andExpect(jsonPath("$.feedback.visual").exists());
    }

    @Test
    void getCollectionsByCollector_WithValidId_ShouldReturnCollections() throws Exception {
        // Arrange
        CollectionResponseDto responseDto = new CollectionResponseDto(mockCollection);
        List<CollectionResponseDto> mockCollections = Arrays.asList(responseDto);

        when(collectionService.getCollectionsByCollectorAsDto(100L))
                .thenReturn(mockCollections);

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/collector/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].binId").value("BIN-001"))
                .andExpect(jsonPath("$[0].location").value("123 Main Street"));
    }

    @Test
    void getCollectionsByCollector_WithServiceError_ShouldReturnError() throws Exception {
        // Arrange
        when(collectionService.getCollectionsByCollectorAsDto(100L))
                .thenThrow(new RuntimeException("Collector not found"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/collector/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Collector not found"));
    }

    @Test
    void getCollectorStats_ShouldReturnStatistics() throws Exception {
        // Arrange
        List<CollectionEvent> mockCollections = Arrays.asList(mockCollection);
        when(collectionService.getCollectionsByCollector(100L))
                .thenReturn(mockCollections);

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/stats/collector/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").exists())
                .andExpect(jsonPath("$.totalWeight").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.todayCollections").exists())
                .andExpect(jsonPath("$.efficiency").exists());
    }

    @Test
    void getTotalWasteCollected_WithValidDates_ShouldReturnTotal() throws Exception {
        // Arrange
        when(collectionService.getTotalWasteCollectedBetween(any(), any()))
                .thenReturn(500.0);
        when(collectionService.getCollectionCountBetween(any(), any()))
                .thenReturn(25L);

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/stats/total-waste")
                        .param("start", "2024-01-01T00:00:00")
                        .param("end", "2024-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWeight").value(500.0))
                .andExpect(jsonPath("$.collectionCount").value(25));
    }

    @Test
    void debugCollectionsByCollector_ShouldReturnDebugInfo() throws Exception {
        // Arrange
        when(collectionService.getCollectionsByCollector(100L))
                .thenReturn(Arrays.asList(mockCollection));

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/debug/collector/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].binId").value("BIN-001"))
                .andExpect(jsonPath("$[0].location").value("123 Main Street"));
    }

    @Test
    void getCollectionSummary_ShouldReturnSummary() throws Exception {
        // Arrange
        when(collectionService.getCollectionSummary("BIN-001"))
                .thenReturn(new Object() {
                    public final String binId = "BIN-001";
                    public final String location = "Test Location";
                    public final Double currentLevel = 75.0;
                    public final boolean scheduledToday = true;
                });

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/summary/BIN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binId").value("BIN-001"))
                .andExpect(jsonPath("$.location").value("Test Location"))
                .andExpect(jsonPath("$.currentLevel").value(75.0))
                .andExpect(jsonPath("$.scheduledToday").value(true));
    }

    @Test
    void getRecyclingCredits_ShouldReturnCredits() throws Exception {
        // Arrange
        when(collectionService.getResidentRecyclingCredits(1L))
                .thenReturn(150.0);

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/recycling/credits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits").value(150.0));
    }

    @Test
    void getCollectionsByBin_ShouldReturnCollections() throws Exception {
        // Arrange
        when(collectionService.getCollectionsByBin("BIN-001"))
                .thenReturn(Arrays.asList(mockCollection));

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/bin/BIN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getUninvoicedCollections_ShouldReturnUninvoiced() throws Exception {
        // Arrange
        when(collectionService.getUninvoicedCollections())
                .thenReturn(Arrays.asList(mockCollection));

        // Act & Assert
        mockMvc.perform(get("/api/waste/collections/uninvoiced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collections").isArray());
    }
}