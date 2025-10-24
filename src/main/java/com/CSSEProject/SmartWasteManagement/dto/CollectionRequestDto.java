// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/dto/CollectionRequestDto.java
package com.CSSEProject.SmartWasteManagement.dto;

import lombok.Data;
import java.util.List;

@Data
public class CollectionRequestDto {
    // Existing fields
    private String binId;
    private Long collectorId;
    private Double weight;           // General waste weight
    private String truckId;
    private boolean offlineMode;
    private String deviceId;

    // ADD THIS MISSING FIELD:
    private String rfidTag; // For RFID-based collection

    // NEW: Recycling fields
    private List<RecyclableItemDto> recyclables; // Recyclable items collected
    private Double totalRecyclableWeight;        // Auto-calculated
    private Double totalRefundAmount;            // Auto-calculated

    // Helper methods
    public boolean hasRecyclables() {
        return recyclables != null && !recyclables.isEmpty();
    }

    public Double calculateTotalRecyclableWeight() {
        if (!hasRecyclables()) return 0.0;
        return recyclables.stream()
                .mapToDouble(RecyclableItemDto::getWeightKg)
                .sum();
    }
}