// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/strategy/AllTimeStrategy.java
package com.CSSEProject.SmartWasteManagement.analytics.strategy;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * All Time Strategy - Strategy Pattern implementation
 * Calculates date range for all available data
 */
@Component
public class AllTimeStrategy implements FilterStrategy {
    
    @Override
    public LocalDateTime getStartDate() {
        // Return a very early date to include all data
        return LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    }
    
    @Override
    public LocalDateTime getEndDate() {
        return LocalDateTime.now();
    }
    
    @Override
    public String getStrategyName() {
        return "All Time";
    }
}
