package com.CSSEProject.SmartWasteManagement.staff.service;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import com.CSSEProject.SmartWasteManagement.waste.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StaffService {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ScheduleService scheduleService;

    public Map<String, Object> getStaffDashboardData(Long staffId) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Get collections data
        List<CollectionEvent> allCollections = collectionService.getCollectionsByCollector(staffId);
        List<CollectionEvent> todayCollections = collectionService.getTodayCollectionsByCollector(staffId);

        // Calculate stats
        dashboardData.put("totalCollections", allCollections.size());
        dashboardData.put("todayCollections", todayCollections.size());
        
        Double todayWeight = todayCollections.stream()
                .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                .sum();
        dashboardData.put("todayWeight", todayWeight);
        
        Double totalWeight = allCollections.stream()
                .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                .sum();
        dashboardData.put("totalWeight", totalWeight);
        
        Double todayRevenue = todayCollections.stream()
                .mapToDouble(c -> c.getCalculatedCharge() != null ? c.getCalculatedCharge() : 0.0)
                .sum();
        dashboardData.put("todayRevenue", todayRevenue);

        // Get pending schedules
        var pendingSchedules = scheduleService.getPendingSchedulesForToday();
        dashboardData.put("pendingSchedules", pendingSchedules);
        dashboardData.put("pendingCollections", pendingSchedules.size());

        // Calculate efficiency (assuming target of 50 collections per day)
        int efficiency = todayCollections.size() > 0 ? Math.round((todayCollections.size() / 50.0f) * 100) : 0;
        dashboardData.put("efficiency", efficiency);

        return dashboardData;
    }
}