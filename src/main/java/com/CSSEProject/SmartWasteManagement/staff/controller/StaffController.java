package com.CSSEProject.SmartWasteManagement.staff.controller;

import com.CSSEProject.SmartWasteManagement.staff.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "http://localhost:5173")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @GetMapping("/dashboard/{staffId}")
    public ResponseEntity<?> getStaffDashboard(@PathVariable Long staffId) {
        try {
            Map<String, Object> dashboardData = staffService.getStaffDashboardData(staffId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}