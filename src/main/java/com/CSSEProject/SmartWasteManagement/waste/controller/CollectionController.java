package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/waste/collections")
@CrossOrigin(origins = "http://localhost:5173")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @PostMapping("/record")
    public ResponseEntity<?> recordCollection(@RequestBody CollectionRequestDto request) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "Collection recorded successfully",
                "collection", collectionService.recordCollection(request)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<?> getCollectionsByCollector(@PathVariable Long collectorId) {
        try {
            return ResponseEntity.ok(collectionService.getCollectionsByCollector(collectorId));
        } catch (RuntimeException e) {
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