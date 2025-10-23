// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/dto/CollectionResponseDto.java
package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CollectionResponseDto {
    // Existing fields
    private Long id;
    private String binId;
    private String location;
    private String residentName;
    private LocalDateTime collectionTime;
    private Double weight;
    private Double calculatedCharge;
    
    // NEW: Recycling fields
    private Double recyclableWeight;
    private Double refundAmount;
    private Integer recyclableItemsCount;
    
    public CollectionResponseDto(CollectionEvent collection) {
        this.id = collection.getId();
        this.binId = collection.getWasteBin() != null ? collection.getWasteBin().getBinId() : null;
        this.location = collection.getWasteBin() != null ? collection.getWasteBin().getLocation() : null;
        this.residentName = collection.getWasteBin() != null && collection.getWasteBin().getResident() != null 
            ? collection.getWasteBin().getResident().getName() : null;
        this.collectionTime = collection.getCollectionTime();
        this.weight = collection.getWeight();
        this.calculatedCharge = collection.getCalculatedCharge();
        
        // Initialize recycling fields (will be populated by service)
        this.recyclableWeight = 0.0;
        this.refundAmount = 0.0;
        this.recyclableItemsCount = 0;
    }
}