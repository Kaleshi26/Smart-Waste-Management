// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/dto/KPIsDto.java
package com.CSSEProject.SmartWasteManagement.analytics.dto;

import lombok.Data;

/**
 * KPIs DTO - Key Performance Indicators data
 * Follows Data Transfer Object pattern
 */
@Data
public class KPIsDto {
    private Double totalWasteCollected;
    private Long totalCollections;
    private Long registeredBins;
    private Double totalRevenue;
}
