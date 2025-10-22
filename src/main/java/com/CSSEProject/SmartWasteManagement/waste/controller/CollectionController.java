package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.CollectionResponseDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import com.CSSEProject.SmartWasteManagement.waste.service.OfflineSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/waste/collections")
@CrossOrigin(origins = "http://localhost:5173")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private OfflineSyncService offlineSyncService;

    // FIXED: Use the DTO method directly for cleaner response
    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<?> getCollectionsByCollector(@PathVariable Long collectorId) {
        try {
            System.out.println("🎯 API CALL: Getting collections for collector " + collectorId);

            List<CollectionResponseDto> collections = collectionService.getCollectionsByCollectorAsDto(collectorId);

            System.out.println("✅ Returning " + collections.size() + " collections for collector " + collectorId);
            if (!collections.isEmpty()) {
                System.out.println("📊 Sample collection: " +
                        collections.get(0).getBinId() + " | " +
                        collections.get(0).getLocation() + " | " +
                        collections.get(0).getCollectionTime());
            }

            return ResponseEntity.ok(collections);
        } catch (RuntimeException e) {
            System.err.println("❌ API ERROR for collector " + collectorId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/record")
    public ResponseEntity<?> recordCollection(@RequestBody CollectionRequestDto request) {
        try {
            // Check if offline mode is requested
            if (request.isOfflineMode()) {
                return recordOfflineCollection(request, request.getDeviceId());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Collection recorded successfully",
                    "collection", collectionService.recordCollection(request),
                    "feedback", Map.of(
                            "audio", "Collection recorded successfully",
                            "visual", "✅ Collection recorded for bin " + request.getBinId()
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "feedback", Map.of(
                            "audio", "Error. " + e.getMessage(),
                            "visual", "❌ " + e.getMessage()
                    )
            ));
        }
    }

    @PostMapping("/record-offline")
    public ResponseEntity<?> recordOfflineCollection(@RequestBody CollectionRequestDto request,
                                                     @RequestHeader(value = "Device-Id", required = false) String deviceId) {
        try {
            if (deviceId == null) {
                deviceId = "device-" + request.getCollectorId();
            }

            var offlineCollection = offlineSyncService.recordOfflineCollection(request, deviceId);
            return ResponseEntity.ok(Map.of(
                    "message", "Collection recorded offline. Will sync when online.",
                    "offlineId", offlineCollection.getId(),
                    "pendingSyncCount", offlineSyncService.getPendingCollectionCount(deviceId),
                    "feedback", Map.of(
                            "audio", "Collection recorded offline",
                            "visual", "📱 Collection saved offline for sync"
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-offline")
    public ResponseEntity<?> syncOfflineCollections(@RequestHeader(value = "Device-Id") String deviceId) {
        try {
            offlineSyncService.syncPendingCollections(deviceId);
            return ResponseEntity.ok(Map.of(
                    "message", "Offline collections synced successfully",
                    "feedback", Map.of(
                            "audio", "Offline collections synced",
                            "visual", "🔄 Offline collections synced to server"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/offline/pending")
    public ResponseEntity<?> getPendingOfflineCollections(@RequestHeader(value = "Device-Id") String deviceId) {
        try {
            var pending = offlineSyncService.getPendingCollections(deviceId);
            return ResponseEntity.ok(Map.of(
                    "pendingCollections", pending,
                    "count", pending.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/collector/{collectorId}")
    public ResponseEntity<?> debugCollectionsByCollector(@PathVariable Long collectorId) {
        try {
            List<CollectionEvent> collections = collectionService.getCollectionsByCollector(collectorId);

            // Debug: Check what data we have
            List<Map<String, Object>> debugData = collections.stream().map(collection -> {
                Map<String, Object> debug = new HashMap<>();
                debug.put("id", collection.getId());
                debug.put("collectionTime", collection.getCollectionTime());
                debug.put("weight", collection.getWeight());
                debug.put("charge", collection.getCalculatedCharge());

                // Check bin data
                if (collection.getWasteBin() != null) {
                    debug.put("binId", collection.getWasteBin().getBinId());
                    debug.put("location", collection.getWasteBin().getLocation());
                    debug.put("binType", collection.getWasteBin().getBinType());
                } else {
                    debug.put("binId", "NULL");
                    debug.put("location", "NULL");
                }

                // Check resident data
                if (collection.getWasteBin() != null && collection.getWasteBin().getResident() != null) {
                    debug.put("residentName", collection.getWasteBin().getResident().getName());
                } else {
                    debug.put("residentName", "NULL");
                }

                return debug;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(debugData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Keep all your existing endpoints...
    @PostMapping("/recycling")
    public ResponseEntity<?> recordRecyclingCollection(@RequestBody RecyclingRequestDto request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "Recycling collection recorded successfully",
                    "recycling", collectionService.recordRecyclingCollection(request)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bin/{binId}")
    public ResponseEntity<?> getCollectionsByBin(@PathVariable String binId) {
        try {
            return ResponseEntity.ok(collectionService.getCollectionsByBin(binId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary/{binId}")
    public ResponseEntity<?> getCollectionSummary(@PathVariable String binId) {
        try {
            Object summary = collectionService.getCollectionSummary(binId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Add this endpoint for staff dashboard stats
    @GetMapping("/stats/collector/{collectorId}")
    public ResponseEntity<?> getCollectorStats(@PathVariable Long collectorId) {
        try {
            List<CollectionEvent> collections = collectionService.getCollectionsByCollector(collectorId);

            // Calculate stats
            Long totalCollections = (long) collections.size();
            Double totalWeight = collections.stream()
                    .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                    .sum();
            Double totalRevenue = collections.stream()
                    .mapToDouble(c -> c.getCalculatedCharge() != null ? c.getCalculatedCharge() : 0.0)
                    .sum();

            // Today's collections
            LocalDate today = LocalDate.now();
            List<CollectionEvent> todayCollections = collections.stream()
                    .filter(c -> c.getCollectionTime() != null &&
                            c.getCollectionTime().toLocalDate().equals(today))
                    .collect(Collectors.toList());

            Double todayWeight = todayCollections.stream()
                    .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                    .sum();
            Double todayRevenue = todayCollections.stream()
                    .mapToDouble(c -> c.getCalculatedCharge() != null ? c.getCalculatedCharge() : 0.0)
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "totalCollections", totalCollections,
                    "totalWeight", totalWeight,
                    "totalRevenue", totalRevenue,
                    "todayCollections", todayCollections.size(),
                    "todayWeight", todayWeight,
                    "todayRevenue", todayRevenue,
                    "efficiency", todayCollections.size() > 0 ? Math.round((todayCollections.size() / 50.0) * 100) : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats/total-waste")
    public ResponseEntity<?> getTotalWasteCollected(@RequestParam String start, @RequestParam String end) {
        try {
            LocalDateTime startDate = LocalDateTime.parse(start);
            LocalDateTime endDate = LocalDateTime.parse(end);

            Double totalWeight = collectionService.getTotalWasteCollectedBetween(startDate, endDate);
            Long collectionCount = collectionService.getCollectionCountBetween(startDate, endDate);

            return ResponseEntity.ok(Map.of(
                    "totalWeight", totalWeight != null ? totalWeight : 0.0,
                    "collectionCount", collectionCount != null ? collectionCount : 0,
                    "period", Map.of("start", start, "end", end)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recycling/credits/{residentId}")
    public ResponseEntity<?> getRecyclingCredits(@PathVariable Long residentId) {
        try {
            Double credits = collectionService.getResidentRecyclingCredits(residentId);
            return ResponseEntity.ok(Map.of("credits", credits != null ? credits : 0.0));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/uninvoiced")
    public ResponseEntity<?> getUninvoicedCollections() {
        try {
            return ResponseEntity.ok(Map.of(
                    "collections", collectionService.getUninvoicedCollections(),
                    "recycling", collectionService.getUninvoicedRecycling()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}