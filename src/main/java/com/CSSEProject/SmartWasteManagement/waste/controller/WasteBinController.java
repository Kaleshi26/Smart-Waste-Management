package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.ScheduleResponseDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.service.ScheduleService;
import com.CSSEProject.SmartWasteManagement.waste.service.WasteBinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/waste/bins")
@CrossOrigin(origins = "http://localhost:5173")
public class WasteBinController {

    @Autowired
    private WasteBinService wasteBinService;
    @Autowired
    private ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<?> createWasteBin(@RequestBody WasteBin wasteBin,
                                            @RequestParam(required = false) Long residentId) {
        try {
            WasteBin createdBin = wasteBinService.createWasteBin(wasteBin, residentId);
            return ResponseEntity.ok(Map.of("message", "Waste bin created successfully", "bin", createdBin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{binId}")
    public ResponseEntity<?> getBinById(@PathVariable String binId) {
        try {
            WasteBin bin = wasteBinService.getBinById(binId);
            return ResponseEntity.ok(bin);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rfid/{rfidTag}")
    public ResponseEntity<?> getBinByRfid(@PathVariable String rfidTag) {
        try {
            WasteBin bin = wasteBinService.getBinByRfid(rfidTag);
            return ResponseEntity.ok(bin);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> getBinsByResident(@PathVariable Long residentId) {
        try {
            return ResponseEntity.ok(wasteBinService.getBinsByResident(residentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getBinsByStatus(@PathVariable String status) {
        try {
            BinStatus binStatus = BinStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(wasteBinService.getBinsByStatus(binStatus));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status or " + e.getMessage()));
        }
    }

    @PutMapping("/{binId}/status")
    public ResponseEntity<?> updateBinStatus(@PathVariable String binId, @RequestBody Map<String, String> request) {
        try {
            BinStatus status = BinStatus.valueOf(request.get("status").toUpperCase());
            WasteBin bin = wasteBinService.updateBinStatus(binId, status);
            return ResponseEntity.ok(Map.of("message", "Bin status updated successfully", "bin", bin));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{binId}/assign-resident")
    public ResponseEntity<?> assignBinToResident(@PathVariable String binId, @RequestBody Map<String, Long> request) {
        try {
            Long residentId = request.get("residentId");
            WasteBin bin = wasteBinService.assignBinToResident(binId, residentId);
            return ResponseEntity.ok(Map.of("message", "Bin assigned to resident successfully", "bin", bin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats/count-by-status")
    public ResponseEntity<?> getBinCountByStatus() {
        try {
            Map<String, Long> stats = Map.of(
                    "active", wasteBinService.getBinCountByStatus(BinStatus.ACTIVE),
                    "needs_emptying", wasteBinService.getBinCountByStatus(BinStatus.NEEDS_EMPTYING),
                    "needs_maintenance", wasteBinService.getBinCountByStatus(BinStatus.NEEDS_MAINTENANCE)
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{binId}/level")
    public ResponseEntity<?> updateBinLevel(@PathVariable String binId, @RequestBody Map<String, Double> request) {
        try {
            Double level = request.get("currentLevel");
            if (level < 0 || level > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Level must be between 0 and 100"));
            }

            WasteBin bin = wasteBinService.updateBinLevel(binId, level);
            return ResponseEntity.ok(Map.of(
                    "message", "Bin level updated successfully",
                    "bin", Map.of(
                            "binId", bin.getBinId(),
                            "currentLevel", bin.getCurrentLevel(),
                            "status", bin.getStatus()
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // FIXED: Use ScheduleResponseDto instead of CollectionSchedule
    @GetMapping("/{binId}/schedule/today")
    public ResponseEntity<?> getTodaySchedule(@PathVariable String binId) {
        try {
            Optional<ScheduleResponseDto> schedule = scheduleService.getTodayScheduleForBin(binId);
            if (schedule.isPresent()) {
                return ResponseEntity.ok(Map.of("scheduled", true, "schedule", schedule.get()));
            } else {
                return ResponseEntity.ok(Map.of("scheduled", false, "message", "No collection scheduled for today"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}