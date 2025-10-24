// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/strategy/FilterStrategy.java
package com.CSSEProject.SmartWasteManagement.analytics.strategy;

import java.time.LocalDateTime;

/**
 * Filter Strategy Interface - Strategy Pattern implementation
 * Each concrete strategy encapsulates different time range calculations
 * 
 * Strategy Pattern: Defines the interface for different filtering strategies
 * This allows the system to switch between different time range calculations
 * without modifying the client code
 */
public interface FilterStrategy {
    /**
     * Get the start date for the filter strategy
     * @return LocalDateTime start date
     */
    LocalDateTime getStartDate();
    
    /**
     * Get the end date for the filter strategy
     * @return LocalDateTime end date
     */
    LocalDateTime getEndDate();
    
    /**
     * Get the name of the strategy
     * @return String strategy name
     */
    String getStrategyName();
}
