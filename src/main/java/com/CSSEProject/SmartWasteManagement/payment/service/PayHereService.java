package com.CSSEProject.SmartWasteManagement.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Service
public class PayHereService {

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @Value("${payhere.mode}")
    private String payhereMode;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    // 🔧 GETTER METHODS - Add these
    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantSecret() {
        return merchantSecret;
    }

    public String getPayhereMode() {
        return payhereMode;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    /**
     * Generate MD5 hash as per PayHere specification
     */
    public String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);

            // Pad with leading zeros to make it 32 characters
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext.toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating MD5 hash", e);
        }
    }

    /**
     * Generate payment hash as per PayHere documentation
     * hash = to_upper_case(md5(merchant_id + order_id + amount + currency + to_upper_case(md5(merchant_secret))))
     */
    public String generatePaymentHash(String orderId, double amount, String currency) {
        try {
            // Format amount to 2 decimal places
            DecimalFormat df = new DecimalFormat("0.00");
            String amountFormatted = df.format(amount);

            // Generate hash of merchant secret
            String hashedSecret = generateMD5(merchantSecret);

            // Generate final hash
            String data = merchantId + orderId + amountFormatted + currency + hashedSecret;
            String hash = generateMD5(data);

            System.out.println("🔐 PAYHERE HASH GENERATION:");
            System.out.println("   - Merchant ID: " + merchantId);
            System.out.println("   - Order ID: " + orderId);
            System.out.println("   - Amount: " + amountFormatted);
            System.out.println("   - Currency: " + currency);
            System.out.println("   - Hashed Secret: " + hashedSecret);
            System.out.println("   - Final Hash: " + hash);

            return hash;
        } catch (Exception e) {
            throw new RuntimeException("Error generating payment hash", e);
        }
    }

    /**
     * Verify webhook signature as per PayHere documentation
     */
    public boolean verifyWebhookSignature(Map<String, String> paymentData) {
        try {
            String receivedHash = paymentData.get("md5sig");
            String merchantId = paymentData.get("merchant_id");
            String orderId = paymentData.get("order_id");
            String amount = paymentData.get("payhere_amount");
            String currency = paymentData.get("payhere_currency");
            String statusCode = paymentData.get("status_code");

            // Generate hash of merchant secret
            String hashedSecret = generateMD5(merchantSecret);

            // Generate local hash
            String data = merchantId + orderId + amount + currency + statusCode + hashedSecret;
            String localHash = generateMD5(data);

            boolean isValid = localHash.equals(receivedHash);

            System.out.println("🔐 WEBHOOK VERIFICATION:");
            System.out.println("   - Received Hash: " + receivedHash);
            System.out.println("   - Local Hash: " + localHash);
            System.out.println("   - Valid: " + isValid);

            return isValid;
        } catch (Exception e) {
            System.err.println("❌ Webhook verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get PayHere checkout URL based on mode
     */
    public String getCheckoutUrl() {
        return "sandbox".equals(payhereMode)
                ? "https://sandbox.payhere.lk/pay/checkout"
                : "https://www.payhere.lk/pay/checkout";
    }

    /**
     * Prepare payment data for PayHere checkout
     */
    public Map<String, String> preparePaymentData(String orderId, String items, double amount,
                                                  String currency, Map<String, String> customerInfo) {
        Map<String, String> paymentData = new HashMap<>();

        // Required parameters
        paymentData.put("merchant_id", merchantId);
        paymentData.put("return_url", frontendUrl + "/payment/success");
        paymentData.put("cancel_url", frontendUrl + "/payment/cancel");
        paymentData.put("notify_url", backendUrl + "/api/payhere/notify");
        paymentData.put("order_id", orderId);
        paymentData.put("items", items);
        paymentData.put("currency", currency);
        paymentData.put("amount", String.format("%.2f", amount));

        // Generate hash
        String hash = generatePaymentHash(orderId, amount, currency);
        paymentData.put("hash", hash);

        // Customer information
        if (customerInfo != null) {
            paymentData.putAll(customerInfo);
        }

        return paymentData;
    }

    /**
     * Get payment status description
     */
    public String getStatusDescription(String statusCode) {
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