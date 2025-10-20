package com.CSSEProject.SmartWasteManagement.payment.controller;

import com.CSSEProject.SmartWasteManagement.dto.BillingModelRequestDto;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "http://localhost:5173")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @PostMapping("/models")
    public ResponseEntity<?> createBillingModel(@RequestBody BillingModelRequestDto requestDto) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "Billing model created successfully",
                "model", billingService.createBillingModel(requestDto)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/models/{modelId}")
    public ResponseEntity<?> updateBillingModel(@PathVariable Long modelId, 
                                               @RequestBody BillingModelRequestDto requestDto) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "Billing model updated successfully",
                "model", billingService.updateBillingModel(modelId, requestDto)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> getActiveBillingModels() {
        try {
            return ResponseEntity.ok(billingService.getActiveBillingModels());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/models/city/{city}")
    public ResponseEntity<?> getBillingModelsByCity(@PathVariable String city) {
        try {
            return ResponseEntity.ok(billingService.getBillingModelsByCity(city));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/models/resident/{residentId}")
    public ResponseEntity<?> getBillingModelForResident(@PathVariable Long residentId) {
        try {
            return ResponseEntity.ok(billingService.getActiveBillingModelForResident(residentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/models/{modelId}")
    public ResponseEntity<?> deactivateBillingModel(@PathVariable Long modelId) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "Billing model deactivated successfully",
                "model", billingService.deactivateBillingModel(modelId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}