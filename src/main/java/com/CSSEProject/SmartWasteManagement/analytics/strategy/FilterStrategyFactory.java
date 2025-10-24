// File: src/main/java/com/CSSEProject/SmartWasteManagement/analytics/strategy/FilterStrategyFactory.java
package com.CSSEProject.SmartWasteManagement.analytics.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter Strategy Factory - Factory Pattern implementation
 * Creates appropriate filter strategies based on the range parameter
 * 
 * Strategy Pattern: Factory method that returns the appropriate strategy
 * This encapsulates the logic for selecting the right strategy
 */
@Component
public class FilterStrategyFactory {
    
    private final Map<String, FilterStrategy> strategies;
    
    @Autowired
    public FilterStrategyFactory(
            Last7DaysStrategy last7DaysStrategy,
            Last30DaysStrategy last30DaysStrategy,
            AllTimeStrategy allTimeStrategy) {
        
        strategies = new HashMap<>();
        strategies.put("7", last7DaysStrategy);
        strategies.put("30", last30DaysStrategy);
        strategies.put("all", allTimeStrategy);
    }
    
    /**
     * Get the appropriate filter strategy based on the range parameter
     * 
     * @param range Time range filter ('7', '30', 'all')
     * @return FilterStrategy appropriate strategy
     * @throws IllegalArgumentException if range is not supported
     */
    public FilterStrategy getStrategy(String range) {
        FilterStrategy strategy = strategies.get(range);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported range: " + range);
        }
        return strategy;
    }
}
