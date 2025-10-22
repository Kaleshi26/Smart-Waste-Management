package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class CollectionResponseDto {
    private Long id;
    private String collectionTime;
    private Double weight;
    private Double calculatedCharge;
    private String binId;
    private String location;
    private String binType;
    private String residentName;
    private Long collectorId;
    private String status = "Completed";

    // Formatter for consistent date display
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    // Enhanced constructor with null checks and formatting
    public CollectionResponseDto(CollectionEvent collection) {
        this.id = collection.getId();

        // Format collection time for frontend
        if (collection.getCollectionTime() != null) {
            this.collectionTime = collection.getCollectionTime().format(formatter);
        } else {
            this.collectionTime = "Unknown";
        }

        this.weight = collection.getWeight() != null ? collection.getWeight() : 0.0;
        this.calculatedCharge = collection.getCalculatedCharge() != null ? collection.getCalculatedCharge() : 0.0;
        this.collectorId = collection.getCollector() != null ? collection.getCollector().getId() : null;

        // Safely handle WasteBin data with detailed logging
        if (collection.getWasteBin() != null) {
            this.binId = collection.getWasteBin().getBinId();
            this.location = collection.getWasteBin().getLocation();
            this.binType = collection.getWasteBin().getBinType() != null ?
                    collection.getWasteBin().getBinType().name() : "UNKNOWN";

            // Safely handle Resident data
            if (collection.getWasteBin().getResident() != null) {
                this.residentName = collection.getWasteBin().getResident().getName();
            } else {
                this.residentName = "Unknown Resident";
                System.out.println("⚠️  Collection " + collection.getId() + ": Bin has no resident");
            }
        } else {
            this.binId = "BIN_NOT_FOUND";
            this.location = "LOCATION_NOT_FOUND";
            this.binType = "UNKNOWN";
            this.residentName = "RESIDENT_NOT_FOUND";
            System.out.println("❌ Collection " + collection.getId() + ": No waste bin associated");
        }

        System.out.println("✅ Created DTO: Collection " + this.id + " | Bin: " + this.binId + " | Location: " + this.location);
    }
}