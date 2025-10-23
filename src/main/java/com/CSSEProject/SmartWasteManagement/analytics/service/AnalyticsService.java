// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/service/AnalyticsService.java
package com.CSSEProject.SmartWasteManagement.analytics.service;

import com.CSSEProject.SmartWasteManagement.analytics.dto.*;
import com.CSSEProject.SmartWasteManagement.analytics.observer.AnalyticsObserver;
import com.CSSEProject.SmartWasteManagement.analytics.strategy.FilterStrategy;
import com.CSSEProject.SmartWasteManagement.analytics.strategy.FilterStrategyFactory;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service - Handles all analytics business logic
 * Follows Single Responsibility Principle - only handles analytics calculations
 * 
 * Strategy Pattern Implementation:
 * - Uses FilterStrategyFactory to get appropriate filtering strategy
 * - Each strategy encapsulates different time range calculations
 * 
 * Observer Pattern Implementation:
 * - Notifies observers when data changes
 * - Allows for real-time updates of analytics components
 */
@Service
@Transactional
public class AnalyticsService {

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private FilterStrategyFactory filterStrategyFactory;

    // Observer Pattern: List of observers for data updates
    private final List<AnalyticsObserver> observers = new ArrayList<>();

    /**
     * Get comprehensive analytics data for the specified time range
     * Strategy Pattern: Uses FilterStrategy to determine date range
     * 
     * @param range Time range filter ('7', '30', 'all')
     * @return Complete analytics data
     */
    public AnalyticsDataDto getAnalyticsData(String range) {
        FilterStrategy strategy = filterStrategyFactory.getStrategy(range);
        LocalDateTime startDate = strategy.getStartDate();
        LocalDateTime endDate = strategy.getEndDate();

        // Get all data using the strategy
        KPIsDto kpis = calculateKPIs(startDate, endDate);
        List<MonthlyDataDto> monthlyData = calculateMonthlyData(startDate, endDate);
        List<CollectionRecordDto> collectionRecords = getCollectionRecords(startDate, endDate);
        List<BinStatusDto> binStatusOverview = getBinStatusOverview();

        AnalyticsDataDto analyticsData = new AnalyticsDataDto();
        analyticsData.setKpis(kpis);
        analyticsData.setMonthlyData(monthlyData);
        analyticsData.setCollectionRecords(collectionRecords);
        analyticsData.setBinStatusOverview(binStatusOverview);

        // Notify observers about data update
        notifyObservers(analyticsData);

        return analyticsData;
    }

    /**
     * Get Key Performance Indicators (KPIs)
     * 
     * @param range Time range filter
     * @return KPIs data
     */
    public KPIsDto getKPIs(String range) {
        FilterStrategy strategy = filterStrategyFactory.getStrategy(range);
        return calculateKPIs(strategy.getStartDate(), strategy.getEndDate());
    }

    /**
     * Get monthly waste collection data for charts
     * 
     * @param range Time range filter
     * @return Monthly data array
     */
    public List<MonthlyDataDto> getMonthlyData(String range) {
        FilterStrategy strategy = filterStrategyFactory.getStrategy(range);
        return calculateMonthlyData(strategy.getStartDate(), strategy.getEndDate());
    }

    /**
     * Get collection records for the specified time range
     * 
     * @param range Time range filter
     * @return Collection records array
     */
    public List<CollectionRecordDto> getCollectionRecords(String range) {
        FilterStrategy strategy = filterStrategyFactory.getStrategy(range);
        return getCollectionRecords(strategy.getStartDate(), strategy.getEndDate());
    }

    /**
     * Get bin status overview data
     * 
     * @return Bin status data array
     */

    /**
     * Export analytics data as CSV
     * 
     * @param range Time range filter
     * @return CSV formatted string
     */
    public String exportToCSV(String range) {
        FilterStrategy strategy = filterStrategyFactory.getStrategy(range);
        List<CollectionRecordDto> records = getCollectionRecords(strategy.getStartDate(), strategy.getEndDate());

        StringBuilder csv = new StringBuilder();
        csv.append("Collection ID,Bin ID,Location,Weight (kg),Collection Date,Staff Member,Charge ($)\n");

        for (CollectionRecordDto record : records) {
            csv.append(String.format("%s,%s,%s,%.2f,%s,%s,%.2f\n",
                    record.getId() != null ? record.getId() : "N/A",
                    record.getBinId() != null ? record.getBinId() : "N/A",
                    record.getLocation() != null ? record.getLocation() : "N/A",
                    record.getWeight() != null ? record.getWeight() : 0.0,
                    record.getCollectionTime() != null ? record.getCollectionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A",
                    record.getStaffName() != null ? record.getStaffName() : "N/A",
                    record.getCalculatedCharge() != null ? record.getCalculatedCharge() : 0.0
            ));
        }

        return csv.toString();
    }

    /**
     * Get real-time analytics summary
     * 
     * @return Summary data map
     */
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Get current day data
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        
        long todayCollections = collectionEventRepository.countByCollectionTimeBetween(startOfDay, endOfDay);
        double todayWeight = collectionEventRepository.findByCollectionTimeBetween(startOfDay, endOfDay)
                .stream()
                .mapToDouble(CollectionEvent::getWeight)
                .sum();
        
        summary.put("todayCollections", todayCollections);
        summary.put("todayWeight", todayWeight);
        summary.put("totalBins", wasteBinRepository.count());
        summary.put("activeBins", wasteBinRepository.countByStatus(com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus.ACTIVE));
        summary.put("lastUpdated", LocalDateTime.now());
        
        return summary;
    }

    // Private helper methods

    private KPIsDto calculateKPIs(LocalDateTime startDate, LocalDateTime endDate) {
        List<CollectionEvent> collections = collectionEventRepository.findByCollectionTimeBetween(startDate, endDate);
        
        double totalWasteCollected = collections.stream()
                .mapToDouble(CollectionEvent::getWeight)
                .sum();
        
        long totalCollections = collections.size();
        
        long registeredBins = wasteBinRepository.count();
        
        double totalRevenue = collections.stream()
                .mapToDouble(CollectionEvent::getCalculatedCharge)
                .sum();
        
        KPIsDto kpis = new KPIsDto();
        kpis.setTotalWasteCollected(totalWasteCollected);
        kpis.setTotalCollections(totalCollections);
        kpis.setRegisteredBins(registeredBins);
        kpis.setTotalRevenue(totalRevenue);
        
        return kpis;
    }

    private List<MonthlyDataDto> calculateMonthlyData(LocalDateTime startDate, LocalDateTime endDate) {
        List<CollectionEvent> collections = collectionEventRepository.findByCollectionTimeBetween(startDate, endDate);
        
        // Group by month
        Map<String, List<CollectionEvent>> monthlyGroups = collections.stream()
                .collect(Collectors.groupingBy(collection -> 
                    collection.getCollectionTime().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        
        List<MonthlyDataDto> monthlyData = new ArrayList<>();
        
        for (Map.Entry<String, List<CollectionEvent>> entry : monthlyGroups.entrySet()) {
            String month = entry.getKey();
            List<CollectionEvent> monthCollections = entry.getValue();
            
            double totalWeight = monthCollections.stream()
                    .mapToDouble(CollectionEvent::getWeight)
                    .sum();
            
            long collectionCount = monthCollections.size();
            
            MonthlyDataDto monthlyDto = new MonthlyDataDto();
            monthlyDto.setMonth(month);
            monthlyDto.setTotalWeight(totalWeight);
            monthlyDto.setCollectionCount(collectionCount);
            
            monthlyData.add(monthlyDto);
        }
        
        // Sort by month
        monthlyData.sort(Comparator.comparing(MonthlyDataDto::getMonth));
        
        return monthlyData;
    }

    private List<CollectionRecordDto> getCollectionRecords(LocalDateTime startDate, LocalDateTime endDate) {
        List<CollectionEvent> collections = collectionEventRepository.findByCollectionTimeBetween(startDate, endDate);
        
        return collections.stream()
                .map(this::convertToCollectionRecordDto)
                .collect(Collectors.toList());
    }

    private CollectionRecordDto convertToCollectionRecordDto(CollectionEvent collection) {
        CollectionRecordDto dto = new CollectionRecordDto();
        dto.setId(collection.getId());
        dto.setBinId(collection.getWasteBin() != null ? collection.getWasteBin().getBinId() : null);
        dto.setLocation(collection.getWasteBin() != null ? collection.getWasteBin().getLocation() : null);
        dto.setWeight(collection.getWeight());
        dto.setCollectionTime(collection.getCollectionTime());
        dto.setStaffName(collection.getCollector() != null ? collection.getCollector().getName() : null);
        dto.setCalculatedCharge(collection.getCalculatedCharge());
        
        return dto;
    }

    public List<BinStatusDto> getBinStatusOverview() {
        List<WasteBin> bins = wasteBinRepository.findAll();
        
        return bins.stream()
                .map(this::convertToBinStatusDto)
                .collect(Collectors.toList());
    }

    private BinStatusDto convertToBinStatusDto(WasteBin bin) {
        BinStatusDto dto = new BinStatusDto();
        dto.setBinId(bin.getBinId());
        dto.setLocation(bin.getLocation());
        dto.setStatus(bin.getStatus().toString());
        dto.setCurrentLevel(bin.getCurrentLevel());
        
        // Get last collection date
        Optional<CollectionEvent> lastCollection = bin.getCollections().stream()
                .max(Comparator.comparing(CollectionEvent::getCollectionTime));
        
        dto.setLastCollectionDate(lastCollection.isPresent() ? 
                lastCollection.get().getCollectionTime().toLocalDate() : null);
        
        return dto;
    }

    // Observer Pattern methods
    public void addObserver(AnalyticsObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AnalyticsObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(AnalyticsDataDto data) {
        for (AnalyticsObserver observer : observers) {
            observer.onDataUpdate(data);
        }
    }
}
