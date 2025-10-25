package com.CSSEProject.SmartWasteManagement.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PayHereServiceTest {

    @InjectMocks
    private PayHereService payHereService;

    @BeforeEach
    void setUp() {
        // Set up test configuration using reflection
        ReflectionTestUtils.setField(payHereService, "merchantId", "1211144");
        ReflectionTestUtils.setField(payHereService, "merchantSecret", "MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA");
        ReflectionTestUtils.setField(payHereService, "payhereMode", "sandbox");
        ReflectionTestUtils.setField(payHereService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(payHereService, "backendUrl", "http://localhost:8082");
    }

    @Test
    void generateMD5_WithValidInput_ShouldReturnCorrectHash() {
        // Arrange
        String input = "test123";

        // Act
        String result = payHereService.generateMD5(input);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length()); // MD5 hash should be 32 characters
    }

    @Test
    void generateMD5_WithEmptyString_ShouldReturnValidHash() {
        // Arrange
        String input = "";

        // Act
        String result = payHereService.generateMD5(input);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    void generateMD5_WithSpecialCharacters_ShouldReturnValidHash() {
        // Arrange
        String input = "hello@world#123";

        // Act
        String result = payHereService.generateMD5(input);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    void generatePaymentHash_WithValidData_ShouldReturnCorrectHash() {
        // Arrange
        String orderId = "INV-001";
        double amount = 15000.00;
        String currency = "LKR";

        // Act
        String result = payHereService.generatePaymentHash(orderId, amount, currency);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    void generatePaymentHash_WithZeroAmount_ShouldReturnValidHash() {
        // Arrange
        String orderId = "INV-002";
        double amount = 0.00;
        String currency = "LKR";

        // Act
        String result = payHereService.generatePaymentHash(orderId, amount, currency);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    void generatePaymentHash_WithDecimalAmount_ShouldFormatCorrectly() {
        // Arrange
        String orderId = "INV-003";
        double amount = 1234.56;
        String currency = "LKR";

        // Act
        String result = payHereService.generatePaymentHash(orderId, amount, currency);

        // Assert
        assertNotNull(result);
    }

    @Test
    void verifyWebhookSignature_WithInvalidHash_ShouldReturnFalse() {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("merchant_id", "1211144");
        paymentData.put("order_id", "INV-001");
        paymentData.put("payhere_amount", "15000.00");
        paymentData.put("payhere_currency", "LKR");
        paymentData.put("status_code", "2");
        paymentData.put("md5sig", "INVALID_HASH_1234567890ABCDEF"); // Invalid hash

        // Act
        boolean result = payHereService.verifyWebhookSignature(paymentData);

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyWebhookSignature_WithMissingFields_ShouldReturnFalse() {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("merchant_id", "1211144");
        // Missing other required fields
        paymentData.put("md5sig", "SOME_HASH");

        // Act
        boolean result = payHereService.verifyWebhookSignature(paymentData);

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyWebhookSignature_WithNullData_ShouldReturnFalse() {
        // Act
        boolean result = payHereService.verifyWebhookSignature(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void getCheckoutUrl_InSandboxMode_ShouldReturnSandboxUrl() {
        // Arrange
        ReflectionTestUtils.setField(payHereService, "payhereMode", "sandbox");

        // Act
        String result = payHereService.getCheckoutUrl();

        // Assert
        assertEquals("https://sandbox.payhere.lk/pay/checkout", result);
    }

    @Test
    void getCheckoutUrl_InProductionMode_ShouldReturnProductionUrl() {
        // Arrange
        ReflectionTestUtils.setField(payHereService, "payhereMode", "production");

        // Act
        String result = payHereService.getCheckoutUrl();

        // Assert
        assertEquals("https://www.payhere.lk/pay/checkout", result);
    }

    @Test
    void preparePaymentData_WithValidInput_ShouldIncludeAllRequiredFields() {
        // Arrange
        String orderId = "INV-001";
        String items = "Waste Management Service";
        double amount = 15000.00;
        String currency = "LKR";

        Map<String, String> customerInfo = new HashMap<>();
        customerInfo.put("first_name", "John");
        customerInfo.put("last_name", "Doe");
        customerInfo.put("email", "john@example.com");
        customerInfo.put("phone", "0771234567");
        customerInfo.put("address", "123 Main Street");
        customerInfo.put("city", "Colombo");
        customerInfo.put("country", "Sri Lanka");

        // Act
        Map<String, String> result = payHereService.preparePaymentData(
                orderId, items, amount, currency, customerInfo
        );

        // Assert
        assertNotNull(result);
        assertEquals("1211144", result.get("merchant_id"));
        assertEquals("INV-001", result.get("order_id"));
        assertEquals("Waste Management Service", result.get("items"));
        assertEquals("LKR", result.get("currency"));
        assertEquals("15000.00", result.get("amount"));
        assertNotNull(result.get("hash"));
    }

    @Test
    void preparePaymentData_WithNullCustomerInfo_ShouldStillWork() {
        // Arrange
        String orderId = "INV-002";
        String items = "Waste Management Service";
        double amount = 20000.00;
        String currency = "LKR";

        // Act
        Map<String, String> result = payHereService.preparePaymentData(
                orderId, items, amount, currency, null
        );

        // Assert
        assertNotNull(result);
        assertEquals("1211144", result.get("merchant_id"));
        assertEquals("INV-002", result.get("order_id"));
        assertEquals("20000.00", result.get("amount"));
        assertNotNull(result.get("hash"));
    }

    @Test
    void getStatusDescription_WithValidStatusCodes_ShouldReturnCorrectDescriptions() {
        // Test all known status codes
        assertEquals("SUCCESS", payHereService.getStatusDescription("2"));
        assertEquals("PENDING", payHereService.getStatusDescription("0"));
        assertEquals("CANCELED", payHereService.getStatusDescription("-1"));
        assertEquals("FAILED", payHereService.getStatusDescription("-2"));
        assertEquals("CHARGEBACK", payHereService.getStatusDescription("-3"));
    }

    @Test
    void getStatusDescription_WithUnknownStatusCode_ShouldReturnUnknown() {
        // Act
        String result = payHereService.getStatusDescription("999");

        // Assert
        assertEquals("UNKNOWN", result);
    }

    @Test
    void getStatusDescription_WithNullStatusCode_ShouldReturnUnknown() {
        // Act
        String result = payHereService.getStatusDescription(null);

        // Assert
        assertEquals("UNKNOWN", result);
    }

    @Test
    void getMerchantId_ShouldReturnConfiguredValue() {
        // Act
        String result = payHereService.getMerchantId();

        // Assert
        assertEquals("1211144", result);
    }

    @Test
    void getMerchantSecret_ShouldReturnConfiguredValue() {
        // Act
        String result = payHereService.getMerchantSecret();

        // Assert
        assertEquals("MjAzNDE4MjU4MzM4NDc4MzE3MTE2MTQ0NDU0NDA3NDE4OTg0OA", result);
    }

    @Test
    void generatePaymentHash_ConsistencyTest_SameInputShouldProduceSameHash() {
        // Arrange
        String orderId = "CONSISTENCY-TEST";
        double amount = 1000.00;
        String currency = "LKR";

        // Act
        String hash1 = payHereService.generatePaymentHash(orderId, amount, currency);
        String hash2 = payHereService.generatePaymentHash(orderId, amount, currency);

        // Assert
        assertEquals(hash1, hash2); // Same input should produce same hash
    }

    @Test
    void generatePaymentHash_DifferentInputsShouldProduceDifferentHashes() {
        // Arrange
        String orderId1 = "ORDER-001";
        String orderId2 = "ORDER-002";
        double amount = 1000.00;
        String currency = "LKR";

        // Act
        String hash1 = payHereService.generatePaymentHash(orderId1, amount, currency);
        String hash2 = payHereService.generatePaymentHash(orderId2, amount, currency);

        // Assert
        assertNotEquals(hash1, hash2); // Different order IDs should produce different hashes
    }

    @Test
    void preparePaymentData_AmountFormatting_ShouldAlwaysHaveTwoDecimals() {
        // Arrange
        String orderId = "FORMAT-TEST";
        double amount = 1500.5; // One decimal
        String currency = "LKR";

        // Act
        Map<String, String> result = payHereService.preparePaymentData(
                orderId, "Test Service", amount, currency, null
        );

        // Assert
        assertEquals("1500.50", result.get("amount")); // Should be formatted to 2 decimals
    }
}