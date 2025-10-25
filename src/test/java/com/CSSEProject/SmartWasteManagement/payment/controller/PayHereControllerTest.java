package com.CSSEProject.SmartWasteManagement.payment.controller;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.payment.service.PayHereService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PayHereControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PayHereService payHereService;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PayHereController payHereController;

    private ObjectMapper objectMapper;
    private Invoice mockInvoice;
    private User mockResident;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(payHereController).build();
        objectMapper = new ObjectMapper();

        // Setup mock resident
        mockResident = new User();
        mockResident.setId(1L);
        mockResident.setName("John Doe");
        mockResident.setEmail("john@example.com");
        mockResident.setPhone("0771234567");
        mockResident.setAddress("123 Main Street, Colombo");

        // Setup mock invoice
        mockInvoice = new Invoice();
        mockInvoice.setId(1L);
        mockInvoice.setInvoiceNumber("INV-001");
        mockInvoice.setFinalAmount(50.0);
        mockInvoice.setStatus(InvoiceStatus.PENDING);
        mockInvoice.setResident(mockResident);
    }

    @Test
    void initiatePayment_WithValidInvoice_ShouldReturnPaymentData() throws Exception {
        // Arrange
        when(invoiceService.getInvoiceById(1L)).thenReturn(mockInvoice);
        
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("merchant_id", "1211144");
        paymentData.put("return_url", "http://localhost:5173/payment/success");
        paymentData.put("cancel_url", "http://localhost:5173/payment/cancel");
        paymentData.put("notify_url", "http://localhost:8082/api/payhere/notify");
        paymentData.put("order_id", "INV-001");
        paymentData.put("items", "Waste Management Service - INV-001");
        paymentData.put("currency", "LKR");
        paymentData.put("amount", "15000.00");
        paymentData.put("hash", "test_hash_value");
        
        when(payHereService.preparePaymentData(
            eq("INV-001"),
            eq("Waste Management Service - INV-001"),
            eq(15000.0), // 50.0 USD * 300 LKR/USD
            eq("LKR"),
            any(Map.class)
        )).thenReturn(paymentData);
        
        when(payHereService.getCheckoutUrl()).thenReturn("https://sandbox.payhere.lk/pay/checkout");

        // Act & Assert
        mockMvc.perform(post("/api/payhere/initiate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://sandbox.payhere.lk/pay/checkout"))
                .andExpect(jsonPath("$.paymentData.merchant_id").value("1211144"))
                .andExpect(jsonPath("$.paymentData.order_id").value("INV-001"))
                .andExpect(jsonPath("$.paymentData.amount").value("15000.00"));

        verify(invoiceService).getInvoiceById(1L);
        verify(payHereService).preparePaymentData(anyString(), anyString(), anyDouble(), anyString(), any(Map.class));
    }

    @Test
    void initiatePayment_WithPaidInvoice_ShouldReturnError() throws Exception {
        // Arrange
        mockInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceService.getInvoiceById(1L)).thenReturn(mockInvoice);

        // Act & Assert
        mockMvc.perform(post("/api/payhere/initiate/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice already paid"));

        verify(invoiceService).getInvoiceById(1L);
        verify(payHereService, never()).preparePaymentData(any(), any(), anyDouble(), any(), any());
    }

    @Test
    void initiatePayment_WithNonExistentInvoice_ShouldReturnError() throws Exception {
        // Arrange
        when(invoiceService.getInvoiceById(999L))
                .thenThrow(new RuntimeException("Invoice not found"));

        // Act & Assert
        mockMvc.perform(post("/api/payhere/initiate/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment initiation failed: Invoice not found"));
    }

    @Test
    void handleNotification_WithValidPayment_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("order_id", "INV-001");
        paymentData.put("status_code", "2");
        paymentData.put("payment_id", "PAY123456");
        paymentData.put("payhere_amount", "15000.00");
        paymentData.put("payhere_currency", "LKR");
        paymentData.put("method", "VISA");
        paymentData.put("md5sig", "valid_signature");

        when(payHereService.verifyWebhookSignature(paymentData)).thenReturn(true);
        doNothing().when(invoiceService).markInvoiceAsPaid("INV-001", "PAY123456");
        when(payHereService.getStatusDescription("2")).thenReturn("Payment successful");

        // Act & Assert
        mockMvc.perform(post("/api/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("order_id", "INV-001")
                .param("status_code", "2")
                .param("payment_id", "PAY123456")
                .param("payhere_amount", "15000.00")
                .param("payhere_currency", "LKR")
                .param("method", "VISA")
                .param("md5sig", "valid_signature"))
                .andExpect(status().isOk())
                .andExpect(content().string("Callback processed"));

        verify(payHereService).verifyWebhookSignature(paymentData);
        verify(invoiceService).markInvoiceAsPaid("INV-001", "PAY123456");
    }

    @Test
    void handleNotification_WithInvalidSignature_ShouldReturnError() throws Exception {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("order_id", "INV-001");
        paymentData.put("status_code", "2");
        paymentData.put("md5sig", "invalid_signature");

        when(payHereService.verifyWebhookSignature(paymentData)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("order_id", "INV-001")
                .param("status_code", "2")
                .param("md5sig", "invalid_signature"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));

        verify(payHereService).verifyWebhookSignature(paymentData);
        verify(invoiceService, never()).markInvoiceAsPaid(anyString(), anyString());
    }

    @Test
    void handleNotification_WithFailedPayment_ShouldLogStatus() throws Exception {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("order_id", "INV-001");
        paymentData.put("status_code", "-1");
        paymentData.put("md5sig", "valid_signature");

        when(payHereService.verifyWebhookSignature(paymentData)).thenReturn(true);
        when(payHereService.getStatusDescription("-1")).thenReturn("Payment canceled");

        // Act & Assert
        mockMvc.perform(post("/api/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("order_id", "INV-001")
                .param("status_code", "-1")
                .param("md5sig", "valid_signature"))
                .andExpect(status().isOk())
                .andExpect(content().string("Callback processed"));

        verify(payHereService).verifyWebhookSignature(paymentData);
        verify(invoiceService, never()).markInvoiceAsPaid(anyString(), anyString());
    }

    @Test
    void testIntegration_ShouldReturnTestData() throws Exception {
        // Arrange
        when(payHereService.generatePaymentHash("OrderNo12345", 1000.00, "LKR"))
                .thenReturn("test_hash_value");
        when(payHereService.getMerchantId()).thenReturn("1211144");
        when(payHereService.getCheckoutUrl()).thenReturn("https://sandbox.payhere.lk/pay/checkout");

        // Act & Assert
        mockMvc.perform(get("/api/payhere/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value("1211144"))
                .andExpect(jsonPath("$.orderId").value("OrderNo12345"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.generatedHash").value("test_hash_value"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://sandbox.payhere.lk/pay/checkout"))
                .andExpect(jsonPath("$.status").value("PayHere integration is working correctly"));
    }

    @Test
    void verifySetup_ShouldReturnConfigurationDetails() throws Exception {
        // Arrange
        when(payHereService.generatePaymentHash("OrderNo12345", 1000.00, "LKR"))
                .thenReturn("actual_test_hash");
        when(payHereService.getMerchantId()).thenReturn("1211144");
        when(payHereService.getCheckoutUrl()).thenReturn("https://sandbox.payhere.lk/pay/checkout");

        // Act & Assert
        mockMvc.perform(get("/api/payhere/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setup").value("PayHere integration is configured correctly"))
                .andExpect(jsonPath("$.merchantId").value("1211144"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://sandbox.payhere.lk/pay/checkout"))
                .andExpect(jsonPath("$.testHash.actual").value("actual_test_hash"))
                .andExpect(jsonPath("$.urls.returnUrl").value("http://localhost:5173/payment/success"))
                .andExpect(jsonPath("$.urls.cancelUrl").value("http://localhost:5173/payment/cancel"))
                .andExpect(jsonPath("$.urls.notifyUrl").value("http://localhost:8082/api/payhere/notify"));
    }

    @Test
    void getPaymentStatus_WithValidInvoice_ShouldReturnStatus() throws Exception {
        // Arrange
        mockInvoice.setStatus(InvoiceStatus.PAID);
        mockInvoice.setPaymentDate(LocalDate.now());
        mockInvoice.setPaymentReference("PAY123456");
        mockInvoice.setPaymentMethod("VISA");

        when(invoiceService.getInvoiceByNumber("INV-001")).thenReturn(mockInvoice);

        // Act & Assert
        mockMvc.perform(get("/api/payhere/status/INV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentReference").value("PAY123456"))
                .andExpect(jsonPath("$.finalAmount").value(50.0))
                .andExpect(jsonPath("$.paymentMethod").value("VISA"));
    }

    @Test
    void getPaymentStatus_WithNonExistentInvoice_ShouldReturnError() throws Exception {
        // Arrange
        when(invoiceService.getInvoiceByNumber("INVALID-INV"))
                .thenThrow(new RuntimeException("Invoice not found"));

        // Act & Assert
        mockMvc.perform(get("/api/payhere/status/INVALID-INV"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));
    }

    @Test
    void initiatePayment_WithResidentWithoutEmail_ShouldUseDefaultEmail() throws Exception {
        // Arrange
        mockResident.setEmail(null);
        mockInvoice.setResident(mockResident);
        
        when(invoiceService.getInvoiceById(1L)).thenReturn(mockInvoice);
        
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("merchant_id", "1211144");
        paymentData.put("order_id", "INV-001");
        paymentData.put("amount", "15000.00");
        
        when(payHereService.preparePaymentData(anyString(), anyString(), anyDouble(), anyString(), any(Map.class)))
                .thenReturn(paymentData);
        when(payHereService.getCheckoutUrl()).thenReturn("https://sandbox.payhere.lk/pay/checkout");

        // Act & Assert
        mockMvc.perform(post("/api/payhere/initiate/1"))
                .andExpect(status().isOk());

        // Verify that payment data preparation was called with customer info containing default email
        verify(payHereService).preparePaymentData(
            eq("INV-001"),
            eq("Waste Management Service - INV-001"),
            eq(15000.0),
            eq("LKR"),
            argThat(customerInfo -> 
                customerInfo.containsKey("email") && 
                "customer@example.com".equals(customerInfo.get("email"))
            )
        );
    }

    @Test
    void handleNotification_WithMissingAmount_ShouldStillProcess() throws Exception {
        // Arrange
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("order_id", "INV-001");
        paymentData.put("status_code", "2");
        paymentData.put("payment_id", "PAY123456");
        paymentData.put("md5sig", "valid_signature");
        // Note: payhere_amount is missing

        when(payHereService.verifyWebhookSignature(paymentData)).thenReturn(true);
        doNothing().when(invoiceService).markInvoiceAsPaid("INV-001", "PAY123456");

        // Act & Assert
        mockMvc.perform(post("/api/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("order_id", "INV-001")
                .param("status_code", "2")
                .param("payment_id", "PAY123456")
                .param("md5sig", "valid_signature"))
                .andExpect(status().isOk());

        verify(invoiceService).markInvoiceAsPaid("INV-001", "PAY123456");
    }
}