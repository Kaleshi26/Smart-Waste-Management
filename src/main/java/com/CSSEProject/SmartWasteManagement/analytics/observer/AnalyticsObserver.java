// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/observer/AnalyticsObserver.java
package com.CSSEProject.SmartWasteManagement.analytics.observer;

import com.CSSEProject.SmartWasteManagement.analytics.dto.AnalyticsDataDto;

/**
 * Analytics Observer Interface - Observer Pattern implementation
 * Defines the contract for observers that need to be notified when analytics data changes
 * 
 * Observer Pattern: Allows objects to subscribe to data updates and be notified automatically
 * This enables real-time updates of UI components when data changes
 */
public interface AnalyticsObserver {
    /**
     * Called when analytics data is updated
     * 
     * @param data Updated analytics data
     */
    void onDataUpdate(AnalyticsDataDto data);
}
