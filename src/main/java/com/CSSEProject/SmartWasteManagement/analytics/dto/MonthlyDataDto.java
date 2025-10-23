// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/dto/MonthlyDataDto.java
package com.CSSEProject.SmartWasteManagement.analytics.dto;

import lombok.Data;

/**
 * Monthly Data DTO - Monthly analytics data for charts
 * Follows Data Transfer Object pattern
 */
@Data
public class MonthlyDataDto {
    private String month;
    private Double totalWeight;
    private Long collectionCount;
}
