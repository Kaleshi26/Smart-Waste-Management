package com.CSSEProject.SmartWasteManagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseChecker implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔍 Checking database schema...");
        
        try {
            // Check if collection_events table exists and its structure
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            );
            
            System.out.println("📋 Existing tables:");
            tables.forEach(table -> System.out.println("   - " + table.get("table_name")));
            
            // Check collection_events structure
            try {
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type FROM information_schema.columns " +
                    "WHERE table_name = 'collection_events' ORDER BY ordinal_position"
                );
                
                System.out.println("📊 Collection_events columns:");
                columns.forEach(col -> System.out.println("   - " + col.get("column_name") + " (" + col.get("data_type") + ")"));
            } catch (Exception e) {
                System.out.println("❌ Collection_events table doesn't exist or has issues");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error checking database: " + e.getMessage());
        }
    }
}