// Testing InvoiceService business logic with mocked repository
package com.CSSEProject.SmartWasteManagement.payment.service;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceService business logic.
 * Tests invoice management operations with mocked repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice mockInvoice;
    private User mockResident;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        mockResident = new User();
        mockResident.setId(1L);
        mockResident.setName("John Doe");
        mockResident.setRole(UserRole.ROLE_RESIDENT);

        mockInvoice = new Invoice();
        mockInvoice.setId(1L);
        mockInvoice.setInvoiceNumber("INV-001");
        mockInvoice.setInvoiceDate(LocalDate.now());
        mockInvoice.setDueDate(LocalDate.now().plusDays(30));
        mockInvoice.setStatus(InvoiceStatus.PENDING);
        mockInvoice.setBaseCharge(50.0);
        mockInvoice.setWeightBasedCharge(25.0);
        mockInvoice.setTotalAmount(75.0);
        mockInvoice.setFinalAmount(75.0);
        mockInvoice.setResident(mockResident);
    }

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenInvoiceExists() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act
        Invoice result = invoiceService.getInvoiceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INV-001", result.getInvoiceNumber());
        assertEquals(InvoiceStatus.PENDING, result.getStatus());
        assertEquals(75.0, result.getTotalAmount(), 0.001);
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenInvoiceNotFound() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invoiceService.getInvoiceById(999L);
        });

        assertEquals("Invoice not found with id: 999", exception.getMessage());
        verify(invoiceRepository).findById(999L);
    }

    @Test
    void getInvoicesByResident_ShouldReturnInvoicesList_WhenResidentHasInvoices() {
        // Arrange
        List<Invoice> mockInvoices = Arrays.asList(mockInvoice);
        when(invoiceRepository.findByResidentId(1L)).thenReturn(mockInvoices);

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
        assertEquals(InvoiceStatus.PENDING, result.get(0).getStatus());
        verify(invoiceRepository).findByResidentId(1L);
    }

    @Test
    void getPendingInvoices_ShouldReturnPendingInvoices_WhenInvoicesExist() {
        // Arrange
        List<Invoice> mockInvoices = Arrays.asList(mockInvoice);
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(mockInvoices);

        // Act
        List<Invoice> result = invoiceService.getPendingInvoices();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(InvoiceStatus.PENDING, result.get(0).getStatus());
        verify(invoiceRepository).findByStatus(InvoiceStatus.PENDING);
    }

    @Test
    void processInvoicePayment_ShouldUpdateInvoiceStatus_WhenPaymentSuccessful() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        // Act
        Invoice result = invoiceService.processInvoicePayment(1L, "credit_card", "txn_123");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }
}
