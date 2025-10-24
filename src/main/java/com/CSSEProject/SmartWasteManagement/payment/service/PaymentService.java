package com.CSSEProject.SmartWasteManagement.payment.service;

<<<<<<< HEAD
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
=======
import org.springframework.beans.factory.annotation.Value;
>>>>>>> main
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PaymentService {

    // These values will come from application.properties
    @Value("${payhere.merchant.id:1211144}")
    private String merchantId;

<<<<<<< HEAD
    public List<Invoice> getInvoicesForUser(Long userId) {
        return invoiceRepository.findByUserId(userId);
=======
    @Value("${payhere.secret.key:MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA==}")
    private String secretKey;

    @Value("${payhere.mode:sandbox}")
    private String payhereMode;

    // Get merchant ID for payment requests
    public String getMerchantId() {
        return merchantId;
    }
    public String getSecretKey() {
        return secretKey;
>>>>>>> main
    }

    // Get the correct PayHere URL (sandbox or live)
    public String getPayhereCheckoutUrl() {
        return "sandbox".equals(payhereMode) 
            ? "https://sandbox.payhere.lk/pay/checkout" 
            : "https://www.payhere.lk/pay/checkout";
    }
    public String generatePaymentHash(String orderId, String amount, String currency) {
        try {
            // Ensure amount has exactly 2 decimal places
            double amountValue = Double.parseDouble(amount);
            String formattedAmount = String.format("%.2f", amountValue);

<<<<<<< HEAD
        // Check if the invoice is already paid
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid.");
=======
            // Clean the secret key - remove any == padding
            String cleanSecretKey = secretKey.replace("=", "").trim();

            // PayHere expects the data in specific order
            String data = merchantId + orderId + formattedAmount + currency + cleanSecretKey;

            System.out.println("🔐 HASH GENERATION DETAILS:");
            System.out.println("   - Merchant ID: " + merchantId);
            System.out.println("   - Order ID: " + orderId);
            System.out.println("   - Amount: " + formattedAmount);
            System.out.println("   - Currency: " + currency);
            System.out.println("   - Clean Secret Key: " + cleanSecretKey.substring(0, Math.min(10, cleanSecretKey.length())) + "...");
            System.out.println("   - Data to hash: " + data);

            String hash = generateMD5Hash(data);
            System.out.println("   - Generated Hash: " + hash);

            return hash;
        } catch (Exception e) {
            System.err.println("❌ Hash generation error: " + e.getMessage());
            throw new RuntimeException("Error generating payment hash: " + e.getMessage());
>>>>>>> main
        }
    }

<<<<<<< HEAD
        // For this simulation, we'll just mark it as PAID.
        // A real system would have logic to connect to a payment gateway.
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
=======
    public String generateMD5Hash(String data) {
        try {
            System.out.println("🔐 MD5 HASH DETAILS:");
            System.out.println("   - Input data: '" + data + "'");
            System.out.println("   - Data length: " + data.length());
            System.out.println("   - Data bytes: " + java.util.Arrays.toString(data.getBytes(StandardCharsets.UTF_8)));
>>>>>>> main

            MessageDigest md = MessageDigest.getInstance("MD5");

            // Use UTF-8 encoding explicitly
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            System.out.println("   - Bytes to hash: " + java.util.Arrays.toString(dataBytes));

            byte[] hashBytes = md.digest(dataBytes);

            // Convert to hexadecimal (lowercase, no spaces)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String result = hexString.toString();
            System.out.println("   - Generated hash: " + result);

            return result;

        } catch (Exception e) {
            System.err.println("❌ MD5 hash generation failed: " + e.getMessage());
            throw new RuntimeException("MD5 hash generation failed: " + e.getMessage());
        }
    }

    // Verify that webhook requests are really from PayHere
    public boolean verifyWebhookHash(Map<String, String> paymentData) {
        try {
            String receivedHash = paymentData.get("md5sig");
            String merchantId = paymentData.get("merchant_id");
            String orderId = paymentData.get("order_id");
            String payhereAmount = paymentData.get("payhere_amount");
            String payhereCurrency = paymentData.get("payhere_currency");
            String statusCode = paymentData.get("status_code");

            // Check if all required fields are present
            if (merchantId == null || orderId == null || payhereAmount == null ||
                    payhereCurrency == null || statusCode == null) {
                System.err.println("❌ Missing required parameters in webhook");
                return false;
            }

            // Clean the secret key
            String cleanSecretKey = secretKey.replace("=", "").trim();

            // Create the same hash that PayHere created
            String data = merchantId + orderId + payhereAmount + payhereCurrency + statusCode + cleanSecretKey;
            String calculatedHash = generateMD5Hash(data);

            // Compare hashes to verify authenticity
            boolean isValid = calculatedHash.equals(receivedHash);
            if (!isValid) {
                System.err.println("❌ Hash verification failed");
                System.err.println("   Received: " + receivedHash);
                System.err.println("   Calculated: " + calculatedHash);
            }

            return isValid;

        } catch (Exception e) {
            System.err.println("❌ Webhook hash verification error: " + e.getMessage());
            return false;
        }
    }

    // Convert status codes to human-readable messages
    public String getPaymentStatusDescription(String statusCode) {
        Map<String, String> statusMap = Map.of(
            "2", "SUCCESS",
            "0", "PENDING", 
            "-1", "CANCELED",
            "-2", "FAILED",
            "-3", "CHARGEBACK"
        );
        return statusMap.getOrDefault(statusCode, "UNKNOWN");
    }
}