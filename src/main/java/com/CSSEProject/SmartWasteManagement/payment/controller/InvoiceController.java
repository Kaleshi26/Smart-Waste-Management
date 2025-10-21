package com.CSSEProject.SmartWasteManagement.payment.controller;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // ADD THIS METHOD - Get Invoice Status
    @GetMapping("/{invoiceId}/status")
    public ResponseEntity<?> getInvoiceStatus(@PathVariable Long invoiceId) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            return ResponseEntity.ok(Map.of(
                    "invoiceId", invoice.getId(),
                    "invoiceNumber", invoice.getInvoiceNumber(),
                    "status", invoice.getStatus().toString(),
                    "totalAmount", invoice.getTotalAmount(),
                    "dueDate", invoice.getDueDate()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/generate/{residentId}")
    public ResponseEntity<?> generateMonthlyInvoice(@PathVariable Long residentId) {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "Invoice generated successfully",
                "invoice", invoiceService.generateMonthlyInvoice(residentId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{invoiceId}/pay")
    public ResponseEntity<?> payInvoice(@PathVariable Long invoiceId, @RequestBody Map<String, String> paymentInfo) {
        try {
            String paymentMethod = paymentInfo.get("paymentMethod");
            String transactionId = paymentInfo.get("transactionId");
            
            return ResponseEntity.ok(Map.of(
                "message", "Payment processed successfully",
                "payment", invoiceService.processInvoicePayment(invoiceId, paymentMethod, transactionId)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> getInvoicesByResident(@PathVariable Long residentId) {
        try {
            return ResponseEntity.ok(invoiceService.getInvoicesByResident(residentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvoices() {
        try {
            return ResponseEntity.ok(invoiceService.getPendingInvoices());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueInvoices() {
        try {
            return ResponseEntity.ok(invoiceService.getOverdueInvoices());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getTotalRevenue(@RequestParam String start, @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            
            Double revenue = invoiceService.getTotalRevenueBetween(startDate, endDate);
            return ResponseEntity.ok(Map.of(
                "totalRevenue", revenue != null ? revenue : 0.0,
                "period", Map.of("start", start, "end", end)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}