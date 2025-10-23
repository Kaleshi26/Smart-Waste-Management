// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/dto/BinStatusDto.java
package com.CSSEProject.SmartWasteManagement.analytics.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Bin Status DTO - Bin status overview data
 * Follows Data Transfer Object pattern
 */
@Data
public class BinStatusDto {
    private String binId;
    private String location;
    private String status;
    private Double currentLevel;
    private LocalDate lastCollectionDate;
}
