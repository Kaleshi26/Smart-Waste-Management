// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/controller/AnalyticsController.java
package com.CSSEProject.SmartWasteManagement.analytics.controller;

import com.CSSEProject.SmartWasteManagement.analytics.service.AnalyticsService;
import com.CSSEProject.SmartWasteManagement.analytics.dto.AnalyticsDataDto;
import com.CSSEProject.SmartWasteManagement.analytics.dto.KPIsDto;
import com.CSSEProject.SmartWasteManagement.analytics.dto.MonthlyDataDto;
import com.CSSEProject.SmartWasteManagement.analytics.dto.CollectionRecordDto;
import com.CSSEProject.SmartWasteManagement.analytics.dto.BinStatusDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Analytics Controller - Handles all analytics-related REST endpoints
 * Follows Single Responsibility Principle - only handles analytics requests
 * 
 * Strategy Pattern Implementation:
 * - Different filtering strategies for time ranges (7 days, 30 days, all time)
 * - Each strategy encapsulates the logic for calculating date ranges
 */
@RestController
@RequestMapping("/api/waste/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Get comprehensive analytics data for the specified time range
     * Strategy Pattern: Uses different strategies based on range parameter
     * 
     * @param range Time range filter ('7', '30', 'all')
     * @return Complete analytics data including KPIs, charts, and records
     */
    @GetMapping
    public ResponseEntity<?> getAnalyticsData(@RequestParam(defaultValue = "30") String range) {
        try {
            AnalyticsDataDto analyticsData = analyticsService.getAnalyticsData(range);
            return ResponseEntity.ok(analyticsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Key Performance Indicators (KPIs) data
     * 
     * @param range Time range filter
     * @return KPIs data including totals and metrics
     */
    @GetMapping("/kpis")
    public ResponseEntity<?> getKPIs(@RequestParam(defaultValue = "30") String range) {
        try {
            KPIsDto kpis = analyticsService.getKPIs(range);
            return ResponseEntity.ok(kpis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get monthly waste collection data for charts
     * 
     * @param range Time range filter
     * @return Monthly data array for visualization
     */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyData(@RequestParam(defaultValue = "30") String range) {
        try {
            List<MonthlyDataDto> monthlyData = analyticsService.getMonthlyData(range);
            return ResponseEntity.ok(monthlyData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get collection records for the specified time range
     * 
     * @param range Time range filter
     * @return Collection records array
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getCollectionRecords(@RequestParam(defaultValue = "30") String range) {
        try {
            List<CollectionRecordDto> collectionRecords = analyticsService.getCollectionRecords(range);
            return ResponseEntity.ok(collectionRecords);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get bin status overview data
     * 
     * @return Bin status data array
     */
    @GetMapping("/bin-status")
    public ResponseEntity<?> getBinStatusOverview() {
        try {
            List<BinStatusDto> binStatusOverview = analyticsService.getBinStatusOverview();
            return ResponseEntity.ok(binStatusOverview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Export analytics data in specified format
     * 
     * @param range Time range filter
     * @param format Export format ('csv', 'json')
     * @return File data for download
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportData(
            @RequestParam(defaultValue = "30") String range,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            if ("csv".equalsIgnoreCase(format)) {
                String csvData = analyticsService.exportToCSV(range);
                return ResponseEntity.ok()
                        .header("Content-Type", "text/csv")
                        .header("Content-Disposition", "attachment; filename=waste_analytics.csv")
                        .body(csvData);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported format"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get real-time analytics summary
     * 
     * @return Real-time summary data
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAnalyticsSummary() {
        try {
            Map<String, Object> summary = analyticsService.getAnalyticsSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
