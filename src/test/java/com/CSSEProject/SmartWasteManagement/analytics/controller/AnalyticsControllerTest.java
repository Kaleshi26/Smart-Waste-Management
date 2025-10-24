// Testing AnalyticsController REST endpoints with MockMvc standalone setup
package com.CSSEProject.SmartWasteManagement.analytics.controller;

import com.CSSEProject.SmartWasteManagement.analytics.controller.AnalyticsController;
import com.CSSEProject.SmartWasteManagement.analytics.dto.*;
import com.CSSEProject.SmartWasteManagement.analytics.service.AnalyticsService;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AnalyticsController using MockMvc in standalone mode.
 * Tests REST endpoints without full Spring context for fast execution.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private KPIsDto mockKPIs;
    private AnalyticsDataDto mockAnalyticsData;
    private MonthlyDataDto mockMonthlyData;
    private CollectionRecordDto mockCollectionRecord;
    private BinStatusDto mockBinStatus;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController).build();
        objectMapper = new ObjectMapper();

        // Setup mock KPIs
        mockKPIs = new KPIsDto();
        mockKPIs.setTotalCollections(100);
        mockKPIs.setTotalWasteCollected(500.0);
        mockKPIs.setTotalRevenue(750.0);
        mockKPIs.setTotalBins(25);
        mockKPIs.setActiveBins(20);

        // Setup mock monthly data
        mockMonthlyData = new MonthlyDataDto();
        mockMonthlyData.setMonth("January");
        mockMonthlyData.setWasteCollected(150.0);
        mockMonthlyData.setRevenue(225.0);
        mockMonthlyData.setCollections(30);

        // Setup mock collection record
        mockCollectionRecord = new CollectionRecordDto();
        mockCollectionRecord.setId(1L);
        mockCollectionRecord.setBinId("BIN-001");
        mockCollectionRecord.setWeight(15.5);
        mockCollectionRecord.setCharge(25.0);
        mockCollectionRecord.setCollectionTime(LocalDateTime.now());

        // Setup mock bin status
        mockBinStatus = new BinStatusDto();
        mockBinStatus.setBinId("BIN-001");
        mockBinStatus.setStatus("ACTIVE");
        mockBinStatus.setLevel(75.0);

        // Setup mock analytics data
        mockAnalyticsData = new AnalyticsDataDto();
        mockAnalyticsData.setKpis(mockKPIs);
        mockAnalyticsData.setMonthlyData(Arrays.asList(mockMonthlyData));
        mockAnalyticsData.setCollectionRecords(Arrays.asList(mockCollectionRecord));
        mockAnalyticsData.setBinStatusOverview(Arrays.asList(mockBinStatus));
    }

    @Test
    void getAnalyticsData_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsData("30")).thenReturn(mockAnalyticsData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(100))
                .andExpect(jsonPath("$.kpis.totalWasteCollected").value(500.0))
                .andExpect(jsonPath("$.kpis.totalRevenue").value(750.0))
                .andExpect(jsonPath("$.kpis.totalBins").value(25))
                .andExpect(jsonPath("$.kpis.activeBins").value(20));
    }

    @Test
    void getAnalyticsData_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsData("invalid")).thenThrow(new RuntimeException("Invalid range"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid range"));
    }

    @Test
    void getKPIs_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        when(analyticsService.getKPIs("30")).thenReturn(mockKPIs);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100))
                .andExpect(jsonPath("$.totalWasteCollected").value(500.0))
                .andExpect(jsonPath("$.totalRevenue").value(750.0))
                .andExpect(jsonPath("$.totalBins").value(25))
                .andExpect(jsonPath("$.activeBins").value(20));
    }

    @Test
    void getKPIs_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getKPIs("invalid")).thenThrow(new RuntimeException("Invalid range"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid range"));
    }

    @Test
    void getMonthlyData_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        when(analyticsService.getMonthlyData("30")).thenReturn(Arrays.asList(mockMonthlyData));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/monthly")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("January"))
                .andExpect(jsonPath("$[0].wasteCollected").value(150.0))
                .andExpect(jsonPath("$[0].revenue").value(225.0))
                .andExpect(jsonPath("$[0].collections").value(30));
    }

    @Test
    void getMonthlyData_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getMonthlyData("invalid")).thenThrow(new RuntimeException("Invalid range"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/monthly")
                .param("range", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid range"));
    }

    @Test
    void getCollectionRecords_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        when(analyticsService.getCollectionRecords("30")).thenReturn(Arrays.asList(mockCollectionRecord));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/records")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].binId").value("BIN-001"))
                .andExpect(jsonPath("$[0].weight").value(15.5))
                .andExpect(jsonPath("$[0].charge").value(25.0));
    }

    @Test
    void getCollectionRecords_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getCollectionRecords("invalid")).thenThrow(new RuntimeException("Invalid range"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/records")
                .param("range", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid range"));
    }

    @Test
    void getBinStatusOverview_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        when(analyticsService.getBinStatusOverview()).thenReturn(Arrays.asList(mockBinStatus));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/bin-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].binId").value("BIN-001"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].level").value(75.0));
    }

    @Test
    void getBinStatusOverview_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getBinStatusOverview()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/bin-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Database error"));
    }

    @Test
    void exportData_ShouldReturnCSV_WhenFormatIsCSV() throws Exception {
        // Arrange
        String csvData = "Collection ID,Weight,Charge\n1,15.5,25.0\n2,22.3,35.0";
        when(analyticsService.exportToCSV("30")).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .param("format", "csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=waste_analytics.csv"))
                .andExpect(content().string(csvData));
    }

    @Test
    void exportData_ShouldReturn400_WhenFormatIsNotCSV() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .param("format", "xml")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported format"));
    }

    @Test
    void exportData_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.exportToCSV("invalid")).thenThrow(new RuntimeException("Invalid range"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "invalid")
                .param("format", "csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid range"));
    }

    @Test
    void getAnalyticsSummary_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCollections", 100L);
        summary.put("totalBins", 25L);
        summary.put("activeBins", 20L);
        summary.put("totalUsers", 50L);
        summary.put("totalInvoices", 75L);
        
        when(analyticsService.getAnalyticsSummary()).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100))
                .andExpect(jsonPath("$.totalBins").value(25))
                .andExpect(jsonPath("$.activeBins").value(20))
                .andExpect(jsonPath("$.totalUsers").value(50))
                .andExpect(jsonPath("$.totalInvoices").value(75));
    }

    @Test
    void getAnalyticsSummary_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsSummary()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Database error"));
    }

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void getAnalyticsData_ShouldUseDefaultRange_WhenNoRangeProvided() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsData("30")).thenReturn(mockAnalyticsData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(100));
    }

    @Test
    void getKPIs_ShouldUseDefaultRange_WhenNoRangeProvided() throws Exception {
        // Arrange
        when(analyticsService.getKPIs("30")).thenReturn(mockKPIs);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100));
    }

    @Test
    void getMonthlyData_ShouldUseDefaultRange_WhenNoRangeProvided() throws Exception {
        // Arrange
        when(analyticsService.getMonthlyData("30")).thenReturn(Arrays.asList(mockMonthlyData));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/monthly")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("January"));
    }

    @Test
    void getCollectionRecords_ShouldUseDefaultRange_WhenNoRangeProvided() throws Exception {
        // Arrange
        when(analyticsService.getCollectionRecords("30")).thenReturn(Arrays.asList(mockCollectionRecord));

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/records")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void exportData_ShouldUseDefaultRange_WhenNoRangeProvided() throws Exception {
        // Arrange
        String csvData = "Collection ID,Weight,Charge\n1,15.5,25.0";
        when(analyticsService.exportToCSV("30")).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("format", "csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(csvData));
    }

    @Test
    void exportData_ShouldUseDefaultFormat_WhenNoFormatProvided() throws Exception {
        // Arrange
        String csvData = "Collection ID,Weight,Charge\n1,15.5,25.0";
        when(analyticsService.exportToCSV("30")).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(csvData));
    }

    @Test
    void getAnalyticsData_ShouldHandleEmptyData_WhenNoCollectionsExist() throws Exception {
        // Arrange
        AnalyticsDataDto emptyData = new AnalyticsDataDto();
        KPIsDto emptyKPIs = new KPIsDto();
        emptyKPIs.setTotalCollections(0);
        emptyKPIs.setTotalWasteCollected(0.0);
        emptyKPIs.setTotalRevenue(0.0);
        emptyKPIs.setTotalBins(0);
        emptyKPIs.setActiveBins(0);
        emptyData.setKpis(emptyKPIs);
        emptyData.setMonthlyData(Collections.emptyList());
        emptyData.setCollectionRecords(Collections.emptyList());
        emptyData.setBinStatusOverview(Collections.emptyList());
        
        when(analyticsService.getAnalyticsData("30")).thenReturn(emptyData);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(0))
                .andExpect(jsonPath("$.kpis.totalWasteCollected").value(0.0))
                .andExpect(jsonPath("$.kpis.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.kpis.totalBins").value(0))
                .andExpect(jsonPath("$.kpis.activeBins").value(0));
    }

    @Test
    void getKPIs_ShouldHandleZeroValues_WhenNoDataExists() throws Exception {
        // Arrange
        KPIsDto emptyKPIs = new KPIsDto();
        emptyKPIs.setTotalCollections(0);
        emptyKPIs.setTotalWasteCollected(0.0);
        emptyKPIs.setTotalRevenue(0.0);
        emptyKPIs.setTotalBins(0);
        emptyKPIs.setActiveBins(0);
        
        when(analyticsService.getKPIs("30")).thenReturn(emptyKPIs);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(0))
                .andExpect(jsonPath("$.totalWasteCollected").value(0.0))
                .andExpect(jsonPath("$.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.totalBins").value(0))
                .andExpect(jsonPath("$.activeBins").value(0));
    }

    @Test
    void getMonthlyData_ShouldHandleEmptyList_WhenNoMonthlyDataExists() throws Exception {
        // Arrange
        when(analyticsService.getMonthlyData("30")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/monthly")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCollectionRecords_ShouldHandleEmptyList_WhenNoRecordsExist() throws Exception {
        // Arrange
        when(analyticsService.getCollectionRecords("30")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/records")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBinStatusOverview_ShouldHandleEmptyList_WhenNoBinsExist() throws Exception {
        // Arrange
        when(analyticsService.getBinStatusOverview()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/bin-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void exportData_ShouldHandleEmptyCSV_WhenNoDataExists() throws Exception {
        // Arrange
        String emptyCsv = "Collection ID,Weight,Charge\n";
        when(analyticsService.exportToCSV("30")).thenReturn(emptyCsv);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .param("format", "csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(emptyCsv));
    }

    @Test
    void getAnalyticsSummary_ShouldHandleEmptySummary_WhenNoDataExists() throws Exception {
        // Arrange
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalCollections", 0L);
        emptySummary.put("totalBins", 0L);
        emptySummary.put("activeBins", 0L);
        emptySummary.put("totalUsers", 0L);
        emptySummary.put("totalInvoices", 0L);
        
        when(analyticsService.getAnalyticsSummary()).thenReturn(emptySummary);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(0))
                .andExpect(jsonPath("$.totalBins").value(0))
                .andExpect(jsonPath("$.activeBins").value(0))
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.totalInvoices").value(0));
    }

    @Test
    void getAnalyticsData_ShouldHandleDifferentRanges_WhenValidRangesProvided() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsData("7")).thenReturn(mockAnalyticsData);
        when(analyticsService.getAnalyticsData("30")).thenReturn(mockAnalyticsData);
        when(analyticsService.getAnalyticsData("all")).thenReturn(mockAnalyticsData);

        // Act & Assert - Test 7 days range
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(100));

        // Act & Assert - Test 30 days range
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(100));

        // Act & Assert - Test all time range
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCollections").value(100));
    }

    @Test
    void getKPIs_ShouldHandleDifferentRanges_WhenValidRangesProvided() throws Exception {
        // Arrange
        when(analyticsService.getKPIs("7")).thenReturn(mockKPIs);
        when(analyticsService.getKPIs("30")).thenReturn(mockKPIs);
        when(analyticsService.getKPIs("all")).thenReturn(mockKPIs);

        // Act & Assert - Test 7 days range
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100));

        // Act & Assert - Test 30 days range
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100));

        // Act & Assert - Test all time range
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollections").value(100));
    }

    @Test
    void exportData_ShouldHandleDifferentFormats_WhenValidFormatsProvided() throws Exception {
        // Arrange
        String csvData = "Collection ID,Weight,Charge\n1,15.5,25.0";
        when(analyticsService.exportToCSV("30")).thenReturn(csvData);

        // Act & Assert - Test CSV format
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .param("format", "csv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(csvData));

        // Act & Assert - Test JSON format (should return error)
        mockMvc.perform(get("/api/waste/analytics/export")
                .param("range", "30")
                .param("format", "json")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported format"));
    }

    @Test
    void getAnalyticsData_ShouldHandleNullResponse_WhenServiceReturnsNull() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsData("30")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void getKPIs_ShouldHandleNullResponse_WhenServiceReturnsNull() throws Exception {
        // Arrange
        when(analyticsService.getKPIs("30")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/kpis")
                .param("range", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void getAnalyticsSummary_ShouldHandleNullResponse_WhenServiceReturnsNull() throws Exception {
        // Arrange
        when(analyticsService.getAnalyticsSummary()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/waste/analytics/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
