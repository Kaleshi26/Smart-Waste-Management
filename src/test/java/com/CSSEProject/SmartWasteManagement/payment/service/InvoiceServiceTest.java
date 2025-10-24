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

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenInvoiceExistsWithDifferentStatus() {
        // Arrange
        mockInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act
        Invoice result = invoiceService.getInvoiceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INV-001", result.getInvoiceNumber());
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertEquals(75.0, result.getTotalAmount(), 0.001);
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenInvoiceIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.getInvoiceById(null);
        });
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenInvoiceIdIsZero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.getInvoiceById(0L);
        });
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenInvoiceIdIsNegative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.getInvoiceById(-1L);
        });
    }

    @Test
    void getInvoicesByResident_ShouldReturnEmptyList_WhenResidentHasNoInvoices() {
        // Arrange
        when(invoiceRepository.findByResidentId(999L)).thenReturn(Collections.emptyList());

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(999L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(invoiceRepository).findByResidentId(999L);
    }

    @Test
    void getInvoicesByResident_ShouldReturnMultipleInvoices_WhenResidentHasMultipleInvoices() {
        // Arrange
        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setStatus(InvoiceStatus.PAID);
        invoice2.setTotalAmount(100.0);
        invoice2.setResident(mockResident);

        List<Invoice> mockInvoices = Arrays.asList(mockInvoice, invoice2);
        when(invoiceRepository.findByResidentId(1L)).thenReturn(mockInvoices);

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
        assertEquals("INV-002", result.get(1).getInvoiceNumber());
        verify(invoiceRepository).findByResidentId(1L);
    }

    @Test
    void getInvoicesByResident_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(invoiceRepository.findByResidentId(1L)).thenReturn(null);

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(invoiceRepository).findByResidentId(1L);
    }

    @Test
    void getPendingInvoices_ShouldReturnEmptyList_WhenNoPendingInvoicesExist() {
        // Arrange
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(Collections.emptyList());

        // Act
        List<Invoice> result = invoiceService.getPendingInvoices();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(invoiceRepository).findByStatus(InvoiceStatus.PENDING);
    }

    @Test
    void getPendingInvoices_ShouldReturnMultipleInvoices_WhenMultiplePendingInvoicesExist() {
        // Arrange
        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setStatus(InvoiceStatus.PENDING);
        invoice2.setTotalAmount(100.0);
        invoice2.setResident(mockResident);

        List<Invoice> mockInvoices = Arrays.asList(mockInvoice, invoice2);
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(mockInvoices);

        // Act
        List<Invoice> result = invoiceService.getPendingInvoices();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(InvoiceStatus.PENDING, result.get(0).getStatus());
        assertEquals(InvoiceStatus.PENDING, result.get(1).getStatus());
        verify(invoiceRepository).findByStatus(InvoiceStatus.PENDING);
    }

    @Test
    void getPendingInvoices_ShouldHandleNullResponse_WhenRepositoryReturnsNull() {
        // Arrange
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(null);

        // Act
        List<Invoice> result = invoiceService.getPendingInvoices();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(invoiceRepository).findByStatus(InvoiceStatus.PENDING);
    }

    @Test
    void processInvoicePayment_ShouldUpdateInvoiceStatus_WhenPaymentMethodIsDebitCard() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        // Act
        Invoice result = invoiceService.processInvoicePayment(1L, "debit_card", "txn_456");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void processInvoicePayment_ShouldUpdateInvoiceStatus_WhenPaymentMethodIsBankTransfer() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        // Act
        Invoice result = invoiceService.processInvoicePayment(1L, "bank_transfer", "txn_789");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void processInvoicePayment_ShouldThrowException_WhenInvoiceNotFound() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invoiceService.processInvoicePayment(999L, "credit_card", "txn_123");
        });

        assertEquals("Invoice not found with id: 999", exception.getMessage());
        verify(invoiceRepository).findById(999L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void processInvoicePayment_ShouldThrowException_WhenPaymentMethodIsNull() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.processInvoicePayment(1L, null, "txn_123");
        });
    }

    @Test
    void processInvoicePayment_ShouldThrowException_WhenTransactionIdIsNull() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.processInvoicePayment(1L, "credit_card", null);
        });
    }

    @Test
    void processInvoicePayment_ShouldThrowException_WhenPaymentMethodIsEmpty() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.processInvoicePayment(1L, "", "txn_123");
        });
    }

    @Test
    void processInvoicePayment_ShouldThrowException_WhenTransactionIdIsEmpty() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.processInvoicePayment(1L, "credit_card", "");
        });
    }

    @Test
    void processInvoicePayment_ShouldHandleRepositoryExceptions_WhenSaveFails() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenThrow(new RuntimeException("Database save failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            invoiceService.processInvoicePayment(1L, "credit_card", "txn_123");
        });
        
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void processInvoicePayment_ShouldHandleRepositoryExceptions_WhenFindByIdFails() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            invoiceService.processInvoicePayment(1L, "credit_card", "txn_123");
        });
        
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void getInvoiceById_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            invoiceService.getInvoiceById(1L);
        });
        
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void getInvoicesByResident_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(invoiceRepository.findByResidentId(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            invoiceService.getInvoicesByResident(1L);
        });
        
        verify(invoiceRepository).findByResidentId(1L);
    }

    @Test
    void getPendingInvoices_ShouldHandleRepositoryExceptions_WhenDatabaseErrorOccurs() {
        // Arrange
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            invoiceService.getPendingInvoices();
        });
        
        verify(invoiceRepository).findByStatus(InvoiceStatus.PENDING);
    }

    @Test
    void processInvoicePayment_ShouldUpdateInvoiceWithCorrectPaymentDetails_WhenPaymentSuccessful() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice savedInvoice = invocation.getArgument(0);
            savedInvoice.setStatus(InvoiceStatus.PAID);
            return savedInvoice;
        });

        // Act
        Invoice result = invoiceService.processInvoicePayment(1L, "credit_card", "txn_123");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void getInvoiceById_ShouldReturnInvoiceWithCorrectAmounts_WhenInvoiceHasDifferentAmounts() {
        // Arrange
        mockInvoice.setBaseCharge(100.0);
        mockInvoice.setWeightBasedCharge(50.0);
        mockInvoice.setTotalAmount(150.0);
        mockInvoice.setFinalAmount(150.0);
        
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));

        // Act
        Invoice result = invoiceService.getInvoiceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getBaseCharge(), 0.001);
        assertEquals(50.0, result.getWeightBasedCharge(), 0.001);
        assertEquals(150.0, result.getTotalAmount(), 0.001);
        assertEquals(150.0, result.getFinalAmount(), 0.001);
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void getInvoicesByResident_ShouldReturnInvoicesWithDifferentStatuses_WhenResidentHasMixedInvoices() {
        // Arrange
        Invoice paidInvoice = new Invoice();
        paidInvoice.setId(2L);
        paidInvoice.setInvoiceNumber("INV-002");
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setTotalAmount(100.0);
        paidInvoice.setResident(mockResident);

        Invoice failedInvoice = new Invoice();
        failedInvoice.setId(3L);
        failedInvoice.setInvoiceNumber("INV-003");
        failedInvoice.setStatus(InvoiceStatus.FAILED);
        failedInvoice.setTotalAmount(200.0);
        failedInvoice.setResident(mockResident);

        List<Invoice> mockInvoices = Arrays.asList(mockInvoice, paidInvoice, failedInvoice);
        when(invoiceRepository.findByResidentId(1L)).thenReturn(mockInvoices);

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(InvoiceStatus.PENDING, result.get(0).getStatus());
        assertEquals(InvoiceStatus.PAID, result.get(1).getStatus());
        assertEquals(InvoiceStatus.FAILED, result.get(2).getStatus());
        verify(invoiceRepository).findByResidentId(1L);
    }

    @Test
    void processInvoicePayment_ShouldHandleDifferentPaymentMethods_WhenVariousPaymentMethodsProvided() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        // Test different payment methods
        String[] paymentMethods = {"credit_card", "debit_card", "bank_transfer", "digital_wallet", "cash"};

        for (String paymentMethod : paymentMethods) {
            // Act
            Invoice result = invoiceService.processInvoicePayment(1L, paymentMethod, "txn_" + paymentMethod);

            // Assert
            assertNotNull(result);
            assertEquals(InvoiceStatus.PAID, result.getStatus());
        }

        verify(invoiceRepository, times(paymentMethods.length)).findById(1L);
        verify(invoiceRepository, times(paymentMethods.length)).save(any(Invoice.class));
    }
}
