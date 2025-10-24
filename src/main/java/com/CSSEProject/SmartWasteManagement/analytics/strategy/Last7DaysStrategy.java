// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/strategy/Last7DaysStrategy.java
package com.CSSEProject.SmartWasteManagement.analytics.strategy;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Last 7 Days Strategy - Strategy Pattern implementation
 * Calculates date range for the last 7 days
 */
@Component
public class Last7DaysStrategy implements FilterStrategy {
    
    @Override
    public LocalDateTime getStartDate() {
        return LocalDateTime.now().minusDays(7);
    }
    
    @Override
    public LocalDateTime getEndDate() {
        return LocalDateTime.now();
    }
    
    @Override
    public String getStrategyName() {
        return "Last 7 Days";
    }
}
