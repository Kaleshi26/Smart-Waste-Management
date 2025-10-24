package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.ScheduleRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.ScheduleResponseDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionSchedule;
import com.CSSEProject.SmartWasteManagement.waste.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/waste/schedules")
@CrossOrigin(origins = "http://localhost:5173")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/create")
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequestDto request) {
        try {
            CollectionSchedule schedule = scheduleService.createCollectionSchedule(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Collection scheduled successfully",
                    "schedule", schedule
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bin/{binId}")
    public ResponseEntity<?> getSchedulesByBin(@PathVariable String binId) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByBin(binId);
            return ResponseEntity.ok(schedules);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> getSchedulesByResident(@PathVariable Long residentId) {
        try {
            List<ScheduleResponseDto> schedules = scheduleService.getSchedulesByResident(residentId);
            return ResponseEntity.ok(schedules);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/today/{binId}")
    public ResponseEntity<?> getTodaySchedule(@PathVariable String binId) {
        try {
            Optional<ScheduleResponseDto> schedule = scheduleService.getTodayScheduleForBin(binId);
            if (schedule.isPresent()) {
                return ResponseEntity.ok(schedule.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{scheduleId}/cancel")
    public ResponseEntity<?> cancelSchedule(@PathVariable Long scheduleId) {
        try {
            CollectionSchedule cancelledSchedule = scheduleService.cancelSchedule(scheduleId);
            return ResponseEntity.ok(Map.of(
                    "message", "Schedule cancelled successfully",
                    "schedule", cancelledSchedule
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingSchedules() {
        try {
            List<ScheduleResponseDto> pendingSchedules = scheduleService.getPendingSchedulesForToday();
            return ResponseEntity.ok(pendingSchedules);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Add this endpoint for today's pending schedules with bin details
    @GetMapping("/pending/today")
    public ResponseEntity<?> getTodayPendingSchedules() {
        try {
            List<ScheduleResponseDto> pendingSchedules = scheduleService.getPendingSchedulesForToday();

            // Add bin location and resident name for better frontend display
            List<Map<String, Object>> enhancedSchedules = pendingSchedules.stream()
                    .map(schedule -> {
                        Map<String, Object> enhanced = new HashMap<>();
                        enhanced.put("id", schedule.getId());
                        enhanced.put("binId", schedule.getBinId());
                        enhanced.put("scheduledDate", schedule.getScheduledDate());
                        // REMOVED: scheduledTime
                        enhanced.put("status", schedule.getStatus());
                        enhanced.put("location", schedule.getLocation());
                        enhanced.put("binType", schedule.getBinType());
                        enhanced.put("currentLevel", schedule.getCurrentLevel());
                        return enhanced;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(enhancedSchedules);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}