// File: src/main/java/com/CSSEProject/SmartWasteManagement/reporting/service/ReportingService.java
package com.CSSEProject.SmartWasteManagement.reporting.service;

import com.CSSEProject.SmartWasteManagement.dto.DashboardStatsDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reporting Service - Handles analytics and dashboard reporting functionality
 * Provides methods for generating dashboard statistics and collection reports
 */
@Service
public class ReportingService {

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    /**
     * Get comprehensive dashboard statistics
     * 
     * @return DashboardStatsDto containing all key metrics
     */
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        
        // Get all collections
        List<CollectionEvent> allCollections = collectionEventRepository.findAll();
        
        // Calculate metrics
        long totalCollections = allCollections.size();
        double totalWasteCollected = allCollections.stream()
                .mapToDouble(CollectionEvent::getWeight)
                .sum();
        double totalRevenue = allCollections.stream()
                .mapToDouble(CollectionEvent::getCalculatedCharge)
                .sum();
        long totalBins = wasteBinRepository.count();
        long activeBins = wasteBinRepository.countByStatus(BinStatus.ACTIVE);
        
        // Set values
        stats.setTotalCollections(totalCollections);
        stats.setTotalWasteCollected(totalWasteCollected);
        stats.setTotalRevenue(totalRevenue);
        stats.setTotalBins(totalBins);
        stats.setActiveBins(activeBins);
        
        return stats;
    }

    /**
     * Get all collection events
     * 
     * @return List of all collection events
     */
    public List<CollectionEvent> getCollectionEvents() {
        return collectionEventRepository.findAll();
    }

    /**
     * Get collection events by collector ID
     * 
     * @param collectorId The ID of the collector
     * @return List of collection events for the specified collector
     */
    public List<CollectionEvent> getCollectionEventsByCollector(Long collectorId) {
        return collectionEventRepository.findByCollectorId(collectorId);
    }

    /**
     * Get collection events by bin ID
     * 
     * @param binId The ID of the waste bin
     * @return List of collection events for the specified bin
     */
    public List<CollectionEvent> getCollectionEventsByBin(String binId) {
        return collectionEventRepository.findByWasteBinBinId(binId);
    }
}
