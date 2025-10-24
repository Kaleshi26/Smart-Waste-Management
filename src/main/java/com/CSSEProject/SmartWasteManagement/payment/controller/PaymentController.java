package com.CSSEProject.SmartWasteManagement.payment.controller;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.payment.service.PaymentService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173") // ✅ Updated to port 5173
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Value("${app.frontend.url:http://localhost:5173}") // ✅ Updated to port 5173
    private String frontendUrl;

    // ✅ Exchange rate for USD to LKR conversion
    private static final double USD_TO_LKR_RATE = 300.0;
    @PostMapping("/initiate/{invoiceId}")
    public ResponseEntity<?> initiatePayment(@PathVariable Long invoiceId) {
        try {
            System.out.println("🔄 Initiating payment for invoice: " + invoiceId);

            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            User resident = invoice.getResident();

            if (invoice.getStatus().toString().equals("PAID")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invoice already paid"));
            }

            // Convert USD to LKR for PayHere
            double amountUSD = invoice.getFinalAmount();
            double amountLKR = amountUSD * USD_TO_LKR_RATE;
            String formattedAmount = String.format("%.2f", amountLKR);

            System.out.println("💰 Currency Conversion:");
            System.out.println("   - USD Amount: $" + amountUSD);
            System.out.println("   - LKR Amount: Rs." + formattedAmount);

            // Generate security hash FIRST
            String hash = paymentService.generatePaymentHash(
                    invoice.getInvoiceNumber(),
                    formattedAmount,
                    "LKR"
            );

            // Build payment data for PayHere - CORRECT ORDER
            Map<String, String> paymentData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
            paymentData.put("merchant_id", paymentService.getMerchantId());
            paymentData.put("return_url", frontendUrl + "/payment/success");
            paymentData.put("cancel_url", frontendUrl + "/payment/cancel");
            paymentData.put("notify_url", "http://localhost:8082/api/payments/webhook");
            paymentData.put("order_id", invoice.getInvoiceNumber());
            paymentData.put("items", "Waste Management Service - " + invoice.getInvoiceNumber());
            paymentData.put("currency", "LKR");
            paymentData.put("amount", formattedAmount);
            paymentData.put("hash", hash); // Add hash to the data

            // Customer information
            if (resident != null) {
                paymentData.put("first_name", resident.getName() != null ?
                        resident.getName().split(" ")[0] : "Customer");
                paymentData.put("last_name", resident.getName() != null &&
                        resident.getName().split(" ").length > 1 ? resident.getName().split(" ")[1] : "");
                paymentData.put("email", resident.getEmail() != null ? resident.getEmail() : "customer@example.com");
                paymentData.put("phone", resident.getPhone() != null ? resident.getPhone() : "0770000000");
                paymentData.put("address", resident.getAddress() != null ? resident.getAddress() : "Colombo");
                paymentData.put("city", "Colombo");
                paymentData.put("country", "Sri Lanka");
            }

            System.out.println("✅ Final Payment Data:");
            paymentData.forEach((key, value) -> {
                System.out.println("   - " + key + ": " + value);
            });

            return ResponseEntity.ok(paymentData);

        } catch (Exception e) {
            System.err.println("❌ Payment initiation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment initiation failed: " + e.getMessage()
            ));
        }
    }
    // Add this test method to your PaymentController.java
    @GetMapping("/test-hash")
    public ResponseEntity<?> testHash() {
        try {
            // Test with PayHere's exact example from documentation
            String testMerchantId = "1211144";
            String testOrderId = "OrderNo12345";
            String testAmount = "1000.00";
            String testCurrency = "LKR";
            String testSecretKey = "MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA==";

            String testData = testMerchantId + testOrderId + testAmount + testCurrency + testSecretKey;
            String expectedHash = "e6f5876de1b7b3e0c2a5a5a077c7f6d1"; // This should match PayHere's example

            String actualHash = paymentService.generateMD5Hash(testData);

            System.out.println("🧪 PAYHERE HASH TEST:");
            System.out.println("   - Test Data: " + testData);
            System.out.println("   - Expected: " + expectedHash);
            System.out.println("   - Actual: " + actualHash);
            System.out.println("   - Match: " + expectedHash.equals(actualHash));

            return ResponseEntity.ok(Map.of(
                    "expected", expectedHash,
                    "actual", actualHash,
                    "match", expectedHash.equals(actualHash)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestParam Map<String, String> paymentData) {
        try {
            System.out.println("🔔 PayHere Webhook Received:");
            paymentData.forEach((key, value) -> {
                System.out.println("   - " + key + ": " + value);
            });

            // Verify the hash for security
            if (!paymentService.verifyWebhookHash(paymentData)) {
                System.err.println("❌ Webhook hash verification FAILED - Possible fraud attempt");
                return ResponseEntity.badRequest().body("Invalid hash");
            }

            String orderId = paymentData.get("order_id");
            String statusCode = paymentData.get("status_code");
            String paymentId = paymentData.get("payment_id");
            String amountLKR = paymentData.get("payhere_amount");

            System.out.println("🔍 Processing webhook:");
            System.out.println("   - Order ID: " + orderId);
            System.out.println("   - Status: " + statusCode);
            System.out.println("   - Payment ID: " + paymentId);
            System.out.println("   - Amount (LKR): Rs." + amountLKR);

            // Update invoice status based on payment
            if ("2".equals(statusCode)) { // 2 = Success
                invoiceService.markInvoiceAsPaid(orderId, paymentId);
                System.out.println("✅ Payment SUCCESSFUL for: " + orderId);

                // ✅ Convert LKR back to USD for logging
                if (amountLKR != null) {
                    double amountLKRValue = Double.parseDouble(amountLKR);
                    double amountUSD = amountLKRValue / USD_TO_LKR_RATE;
                    System.out.println("   - Amount Paid (USD): $" + String.format("%.2f", amountUSD));
                }
            } else {
                String statusDesc = paymentService.getPaymentStatusDescription(statusCode);
                System.out.println("❌ Payment " + statusDesc + " for: " + orderId);
            }

            return ResponseEntity.ok("Callback processed");

        } catch (Exception e) {
            System.err.println("❌ Webhook processing error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing webhook");
        }
    }
    @GetMapping("/test-payhere-integration")
    public ResponseEntity<?> testPayHereIntegration() {
        try {
            // Test with PayHere's example data
            String testOrderId = "OrderNo12345";
            String testAmount = "1000.00";
            String testCurrency = "LKR";

            String hash = paymentService.generatePaymentHash(testOrderId, testAmount, testCurrency);

            Map<String, Object> result = new HashMap<>();
            result.put("merchantId", paymentService.getMerchantId());
            result.put("orderId", testOrderId);
            result.put("amount", testAmount);
            result.put("currency", testCurrency);
            result.put("generatedHash", hash);
            result.put("expectedUrl", paymentService.getPayhereCheckoutUrl());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify-credentials")
    public ResponseEntity<?> verifyPayHereCredentials() {
        try {
            // Test with PayHere's exact example
            String testMerchantId = "1211144";
            String testOrderId = "OrderNo12345";
            String testAmount = "1000.00";
            String testCurrency = "LKR";
            String testSecretKey = "MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA"; // No ==

            // Expected hash from PayHere documentation
            String expectedHash = "e6f5876de1b7b3e0c2a5a5a077c7f6d1";

            String testData = testMerchantId + testOrderId + testAmount + testCurrency + testSecretKey;
            String actualHash = paymentService.generateMD5Hash(testData);

            System.out.println("🧪 PAYHERE CREDENTIALS TEST:");
            System.out.println("   - Test Data: " + testData);
            System.out.println("   - Expected Hash: " + expectedHash);
            System.out.println("   - Actual Hash: " + actualHash);
            System.out.println("   - Match: " + expectedHash.equals(actualHash));

            // Test with your actual credentials
            String yourData = paymentService.getMerchantId() + "OrderNo12345" + "1000.00" + "LKR" +
                    paymentService.getSecretKey().replace("=", "").trim();
            String yourHash = paymentService.generateMD5Hash(yourData);

            return ResponseEntity.ok(Map.of(
                    "payhereTest", Map.of(
                            "expected", expectedHash,
                            "actual", actualHash,
                            "match", expectedHash.equals(actualHash)
                    ),
                    "yourCredentials", Map.of(
                            "merchantId", paymentService.getMerchantId(),
                            "secretKey", paymentService.getSecretKey().replace("=", "").trim(),
                            "generatedHash", yourHash,
                            "checkoutUrl", paymentService.getPayhereCheckoutUrl()
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Additional endpoint to check payment status
    @GetMapping("/status/{invoiceNumber}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String invoiceNumber) {
        try {
            Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
            return ResponseEntity.ok(Map.of(
                    "invoiceNumber", invoice.getInvoiceNumber(),
                    "status", invoice.getStatus(),
                    "paymentDate", invoice.getPaymentDate(),
                    "paymentReference", invoice.getPaymentReference(),
                    "finalAmount", invoice.getFinalAmount(), // ✅ Include amount for frontend
                    "paymentMethod", invoice.getPaymentMethod()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}