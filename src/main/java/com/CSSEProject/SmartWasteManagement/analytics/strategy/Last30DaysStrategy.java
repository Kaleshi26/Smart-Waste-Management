// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/strategy/Last30DaysStrategy.java
package com.CSSEProject.SmartWasteManagement.analytics.strategy;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Last 30 Days Strategy - Strategy Pattern implementation
 * Calculates date range for the last 30 days
 */
@Component
public class Last30DaysStrategy implements FilterStrategy {
    
    @Override
    public LocalDateTime getStartDate() {
        return LocalDateTime.now().minusDays(30);
    }
    
    @Override
    public LocalDateTime getEndDate() {
        return LocalDateTime.now();
    }
    
    @Override
    public String getStrategyName() {
        return "Last 30 Days";
    }
}
