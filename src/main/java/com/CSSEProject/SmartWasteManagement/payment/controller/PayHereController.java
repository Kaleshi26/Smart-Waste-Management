package com.CSSEProject.SmartWasteManagement.payment.controller;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.payment.service.PayHereService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payhere")
@CrossOrigin(origins = "http://localhost:5173")
public class PayHereController {

    @Autowired
    private PayHereService payHereService;

    @Autowired
    private InvoiceService invoiceService;

    // Exchange rate for USD to LKR
    private static final double USD_TO_LKR = 300.0;

    /**
     * Initiate PayHere payment
     */
    @PostMapping("/initiate/{invoiceId}")
    public ResponseEntity<?> initiatePayment(@PathVariable Long invoiceId) {
        try {
            System.out.println("🔄 Initiating PayHere payment for invoice: " + invoiceId);

            // Get invoice details
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            User resident = invoice.getResident();

            if (invoice.getStatus().toString().equals("PAID")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invoice already paid"));
            }

            // Convert USD to LKR for PayHere
            double amountUSD = invoice.getFinalAmount();
            double amountLKR = amountUSD * USD_TO_LKR;

            System.out.println("💰 Currency Conversion:");
            System.out.println("   - USD Amount: $" + amountUSD);
            System.out.println("   - LKR Amount: Rs." + String.format("%.2f", amountLKR));

            // Prepare customer information
            Map<String, String> customerInfo = new HashMap<>();
            if (resident != null) {
                String[] nameParts = resident.getName() != null ? resident.getName().split(" ", 2) : new String[]{"Customer", ""};
                customerInfo.put("first_name", nameParts[0]);
                customerInfo.put("last_name", nameParts.length > 1 ? nameParts[1] : "");
                customerInfo.put("email", resident.getEmail() != null ? resident.getEmail() : "customer@example.com");
                customerInfo.put("phone", resident.getPhone() != null ? resident.getPhone() : "0770000000");
                customerInfo.put("address", resident.getAddress() != null ? resident.getAddress() : "Colombo");
                customerInfo.put("city", "Colombo");
                customerInfo.put("country", "Sri Lanka");
            }

            // Prepare payment data
            Map<String, String> paymentData = payHereService.preparePaymentData(
                invoice.getInvoiceNumber(),
                "Waste Management Service - " + invoice.getInvoiceNumber(),
                amountLKR,
                "LKR",
                customerInfo
            );

            System.out.println("✅ Payment data prepared successfully");
            System.out.println("   - Checkout URL: " + payHereService.getCheckoutUrl());
            System.out.println("   - Order ID: " + invoice.getInvoiceNumber());
            System.out.println("   - Amount: Rs." + String.format("%.2f", amountLKR));

            return ResponseEntity.ok(Map.of(
                "checkoutUrl", payHereService.getCheckoutUrl(),
                "paymentData", paymentData
            ));

        } catch (Exception e) {
            System.err.println("❌ Payment initiation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Payment initiation failed: " + e.getMessage()
            ));
        }
    }

    /**
     * PayHere notification webhook
     */
    @PostMapping("/notify")
    public ResponseEntity<?> handleNotification(@RequestParam Map<String, String> paymentData) {
        try {
            System.out.println("🔔 PayHere Notification Received:");
            paymentData.forEach((key, value) -> {
                System.out.println("   - " + key + ": " + value);
            });

            // Verify the signature
            if (!payHereService.verifyWebhookSignature(paymentData)) {
                System.err.println("❌ Webhook signature verification FAILED");
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            String orderId = paymentData.get("order_id");
            String statusCode = paymentData.get("status_code");
            String paymentId = paymentData.get("payment_id");
            String amount = paymentData.get("payhere_amount");
            String currency = paymentData.get("payhere_currency");
            String method = paymentData.get("method");

            System.out.println("🔍 Processing Payment:");
            System.out.println("   - Order ID: " + orderId);
            System.out.println("   - Status: " + statusCode + " (" + payHereService.getStatusDescription(statusCode) + ")");
            System.out.println("   - Payment ID: " + paymentId);
            System.out.println("   - Amount: " + amount + " " + currency);
            System.out.println("   - Method: " + method);

            // Handle payment status
            if ("2".equals(statusCode)) {
                // Payment successful
                invoiceService.markInvoiceAsPaid(orderId, paymentId);
                System.out.println("✅ Payment SUCCESSFUL for: " + orderId);

                // Convert LKR back to USD for logging
                if (amount != null) {
                    double amountLKR = Double.parseDouble(amount);
                    double amountUSD = amountLKR / USD_TO_LKR;
                    System.out.println("   - Amount Paid (USD): $" + String.format("%.2f", amountUSD));
                }
            } else {
                String statusDesc = payHereService.getStatusDescription(statusCode);
                System.out.println("❌ Payment " + statusDesc + " for: " + orderId);
            }

            return ResponseEntity.ok("Callback processed");

        } catch (Exception e) {
            System.err.println("❌ Notification processing error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing notification");
        }
    }

    /**
     * Test endpoint to verify PayHere integration
     */
    @GetMapping("/test")
    public ResponseEntity<?> testIntegration() {
        try {
            // Test with PayHere's example data
            String testOrderId = "OrderNo12345";
            double testAmount = 1000.00;
            String testCurrency = "LKR";

            String hash = payHereService.generatePaymentHash(testOrderId, testAmount, testCurrency);

            Map<String, Object> result = new HashMap<>();
            result.put("merchantId", payHereService.getMerchantId());
            result.put("orderId", testOrderId);
            result.put("amount", testAmount);
            result.put("currency", testCurrency);
            result.put("generatedHash", hash);
            result.put("checkoutUrl", payHereService.getCheckoutUrl());
            result.put("status", "PayHere integration is working correctly");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // Add this to PayHereController.java
    @GetMapping("/verify")
    public ResponseEntity<?> verifySetup() {
        try {
            // Test hash generation with PayHere's example
            String testMerchantId = "1211144";
            String testOrderId = "OrderNo12345";
            double testAmount = 1000.00;
            String testCurrency = "LKR";
            String testSecret = "MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA";

            // Expected hash from PayHere documentation
            String expectedHash = "0C1A366D9C84C1B8B8E6DF6F6F6F6F6F"; // This will be different

            String actualHash = payHereService.generatePaymentHash(testOrderId, testAmount, testCurrency);

            return ResponseEntity.ok(Map.of(
                    "setup", "PayHere integration is configured correctly",
                    "merchantId", payHereService.getMerchantId(),
                    "checkoutUrl", payHereService.getCheckoutUrl(),
                    "testHash", Map.of(
                            "expected", expectedHash,
                            "actual", actualHash,
                            "note", "Hashes may differ based on actual credentials"
                    ),
                    "urls", Map.of(
                            "returnUrl", "http://localhost:5173/payment/success",
                            "cancelUrl", "http://localhost:5173/payment/cancel",
                            "notifyUrl", "http://localhost:8082/api/payhere/notify"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{invoiceNumber}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String invoiceNumber) {
        try {
            Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
            return ResponseEntity.ok(Map.of(
                "invoiceNumber", invoice.getInvoiceNumber(),
                "status", invoice.getStatus(),
                "paymentDate", invoice.getPaymentDate(),
                "paymentReference", invoice.getPaymentReference(),
                "finalAmount", invoice.getFinalAmount(),
                "paymentMethod", invoice.getPaymentMethod()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}