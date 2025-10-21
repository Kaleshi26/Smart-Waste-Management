package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.OfflineCollection;
import com.CSSEProject.SmartWasteManagement.waste.repository.OfflineCollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OfflineSyncService {

    @Autowired
    private OfflineCollectionRepository offlineCollectionRepository;

    @Autowired
    private CollectionService collectionService;

    public void syncPendingCollections(String deviceId) {
        List<OfflineCollection> pendingCollections = 
            offlineCollectionRepository.findByDeviceIdAndSyncedFalse(deviceId);
            
        int successCount = 0;
        int errorCount = 0;
            
        for (OfflineCollection offlineCollection : pendingCollections) {
            try {
                // Convert offline collection to regular collection
                CollectionRequestDto request = new CollectionRequestDto();
                request.setBinId(offlineCollection.getBinId());
                request.setCollectorId(offlineCollection.getCollectorId());
                request.setWeight(offlineCollection.getWeight());
                request.setCollectionTime(offlineCollection.getCollectionTime());
                request.setRfidTag(offlineCollection.getRfidTag());
                
                collectionService.recordCollection(request);
                
                // Mark as synced
                offlineCollection.setSynced(true);
                offlineCollection.setSyncTime(LocalDateTime.now());
                offlineCollectionRepository.save(offlineCollection);
                
                successCount++;
                
            } catch (Exception e) {
                // Log error but continue with other collections
                System.err.println("Failed to sync collection: " + offlineCollection.getId() + " - " + e.getMessage());
                errorCount++;
            }
        }
        
        System.out.println("Sync completed: " + successCount + " successful, " + errorCount + " failed");
    }
    
    public OfflineCollection recordOfflineCollection(CollectionRequestDto request, String deviceId) {
        OfflineCollection offlineCollection = new OfflineCollection();
        offlineCollection.setBinId(request.getBinId());
        offlineCollection.setCollectorId(request.getCollectorId());
        offlineCollection.setWeight(request.getWeight());
        offlineCollection.setRfidTag(request.getRfidTag());
        offlineCollection.setDeviceId(deviceId);
        offlineCollection.setCollectionTime(request.getCollectionTime() != null ? 
            request.getCollectionTime() : LocalDateTime.now());
        
        // Store complete collection data as JSON for backup
        offlineCollection.setCollectionData(createCollectionDataJson(request));
        
        return offlineCollectionRepository.save(offlineCollection);
    }
    
    private String createCollectionDataJson(CollectionRequestDto request) {
        // Simple JSON representation of collection data
        return String.format(
            "{\"binId\":\"%s\",\"collectorId\":%d,\"weight\":%.2f,\"rfidTag\":\"%s\"}",
            request.getBinId(),
            request.getCollectorId(),
            request.getWeight(),
            request.getRfidTag() != null ? request.getRfidTag() : ""
        );
    }
    
    public List<OfflineCollection> getPendingCollections(String deviceId) {
        return offlineCollectionRepository.findByDeviceIdAndSyncedFalse(deviceId);
    }
    
    public Long getPendingCollectionCount(String deviceId) {
        return offlineCollectionRepository.countByDeviceIdAndSyncedFalse(deviceId);
    }
}