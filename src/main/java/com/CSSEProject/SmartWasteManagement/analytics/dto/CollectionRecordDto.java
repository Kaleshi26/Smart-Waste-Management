// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/dto/CollectionRecordDto.java
package com.CSSEProject.SmartWasteManagement.analytics.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Collection Record DTO - Individual collection record data
 * Follows Data Transfer Object pattern
 */
@Data
public class CollectionRecordDto {
    private Long id;
    private String binId;
    private String location;
    private Double weight;
    private LocalDateTime collectionTime;
    private String staffName;
    private Double calculatedCharge;
}
