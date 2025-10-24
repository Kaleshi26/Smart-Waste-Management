package com.CSSEProject.SmartWasteManagement.reporting.controller;

import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import com.CSSEProject.SmartWasteManagement.waste.service.WasteBinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class ReportController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private WasteBinService wasteBinService;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@RequestParam(required = false) String period) {
        try {
            LocalDateTime startDate;
            LocalDateTime endDate = LocalDateTime.now();
            
            if ("7days".equals(period)) {
                startDate = endDate.minusDays(7);
            } else if ("30days".equals(period)) {
                startDate = endDate.minusDays(30);
            } else {
                startDate = endDate.minusYears(10); // All time
            }

            // Collection statistics
            Double totalWaste = collectionService.getTotalWasteCollectedBetween(startDate, endDate);
            Long collectionCount = collectionService.getCollectionCountBetween(startDate, endDate);
            
            // Bin statistics
            Long totalBins = wasteBinService.getBinCountByStatus(com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus.ACTIVE);
            Long binsNeedingEmptying = wasteBinService.getBinCountByStatus(com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus.NEEDS_EMPTYING);

            // Revenue statistics
            LocalDate revenueStart = startDate.toLocalDate();
            LocalDate revenueEnd = endDate.toLocalDate();
            Double totalRevenue = invoiceService.getTotalRevenueBetween(revenueStart, revenueEnd);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalWeightKg", totalWaste != null ? totalWaste : 0.0);
            stats.put("totalCollections", collectionCount != null ? collectionCount : 0);
            stats.put("totalBins", totalBins);
            stats.put("binsNeedingEmptying", binsNeedingEmptying);
            stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            stats.put("period", period != null ? period : "all");

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/monthly-waste")
    public ResponseEntity<?> getMonthlyWasteData(@RequestParam(required = false) Integer year) {
        try {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            
            // Generate monthly data for the year
            Map<String, Object> monthlyData = new HashMap<>();
            
            for (int month = 1; month <= 12; month++) {
                YearMonth yearMonth = YearMonth.of(targetYear, month);
                LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
                LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
                
                Double monthlyWaste = collectionService.getTotalWasteCollectedBetween(start, end);
                monthlyData.put(yearMonth.getMonth().toString(), monthlyWaste != null ? monthlyWaste : 0.0);
            }
            
            return ResponseEntity.ok(monthlyData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/collection-events")
    public ResponseEntity<?> getCollectionEvents(@RequestParam(required = false) String start, 
                                                @RequestParam(required = false) String end) {
        try {
            LocalDateTime startDate = start != null ? LocalDateTime.parse(start) : LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = end != null ? LocalDateTime.parse(end) : LocalDateTime.now();
            
            // This would typically join with other services to get full event details
            // For now, return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("totalEvents", collectionService.getCollectionCountBetween(startDate, endDate));
            response.put("totalWeight", collectionService.getTotalWasteCollectedBetween(startDate, endDate));
            response.put("period", Map.of("start", startDate, "end", endDate));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/financial")
    public ResponseEntity<?> getFinancialReport(@RequestParam String start, @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            
            Double revenue = invoiceService.getTotalRevenueBetween(startDate, endDate);
            Long overdueInvoices = (long) invoiceService.getOverdueInvoices().size();
            
            Map<String, Object> report = new HashMap<>();
            report.put("totalRevenue", revenue != null ? revenue : 0.0);
            report.put("overdueInvoices", overdueInvoices);
            report.put("period", Map.of("start", start, "end", end));
            
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}