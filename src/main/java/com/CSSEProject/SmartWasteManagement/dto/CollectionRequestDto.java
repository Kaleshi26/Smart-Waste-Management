package com.CSSEProject.SmartWasteManagement.dto;

import lombok.Data;

@Data
public class CollectionRequestDto {
    private String binId;
    private String rfidTag;
    private Long collectorId;
    private String truckId;
    private Double weight;
    private boolean offlineMode = false;
    private String deviceId;
    private java.time.LocalDateTime collectionTime;
    private Double gpsLatitude;
    private Double gpsLongitude;

    public CollectionRequestDto() {
        this.collectionTime = java.time.LocalDateTime.now();
    }
}