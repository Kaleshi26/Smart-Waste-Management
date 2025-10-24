// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/dto/AnalyticsDataDto.java
package com.CSSEProject.SmartWasteManagement.analytics.dto;

import lombok.Data;
import java.util.List;

/**
 * Analytics Data DTO - Complete analytics data container
 * Follows Data Transfer Object pattern for clean data transfer
 */
@Data
public class AnalyticsDataDto {
    private KPIsDto kpis;
    private List<MonthlyDataDto> monthlyData;
    private List<CollectionRecordDto> collectionRecords;
    private List<BinStatusDto> binStatusOverview;
}
