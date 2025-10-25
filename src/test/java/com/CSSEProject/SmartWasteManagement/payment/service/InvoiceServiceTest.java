package com.CSSEProject.SmartWasteManagement.payment.service;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.entity.Payment;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import com.CSSEProject.SmartWasteManagement.payment.repository.PaymentRepository;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserService userService;

    @Mock
    private CollectionEventRepository collectionEventRepository;

    @Mock
    private RecyclingCollectionRepository recyclingCollectionRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User testUser;
    private Invoice testInvoice;
    private CollectionEvent testCollection;
    private RecyclingCollection testRecycling;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setPendingCharges(1000.0);
        testUser.setRecyclingCredits(200.0);

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("INV-001");
        testInvoice.setResident(testUser);
        testInvoice.setTotalAmount(1000.0);
        testInvoice.setFinalAmount(800.0);
        testInvoice.setStatus(InvoiceStatus.PENDING);
        testInvoice.setDueDate(LocalDate.now().plusDays(30));

        testCollection = new CollectionEvent();
        testCollection.setId(1L);
        testCollection.setCalculatedCharge(500.0);

        testRecycling = new RecyclingCollection();
        testRecycling.setId(1L);
        testRecycling.setPaybackAmount(200.0);
        testRecycling.setWeight(10.0);
    }

    @Test
    void generateMonthlyInvoice_WithPendingCharges_ShouldGenerateInvoice() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(collectionEventRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList());
        when(recyclingCollectionRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        // Act
        Invoice result = invoiceService.generateMonthlyInvoice(1L);

        // Assert
        assertNotNull(result);
        assertEquals("INV-001", result.getInvoiceNumber());
        assertEquals(InvoiceStatus.PENDING, result.getStatus());
        verify(userService).updateUser(testUser);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void generateMonthlyInvoice_WithCollections_ShouldGenerateInvoice() {
        // Arrange
        testUser.setPendingCharges(0.0);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(collectionEventRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList(testCollection));
        when(recyclingCollectionRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList(testRecycling));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(collectionEventRepository.save(any(CollectionEvent.class))).thenReturn(testCollection);
        when(recyclingCollectionRepository.save(any(RecyclingCollection.class))).thenReturn(testRecycling);

        // Act
        Invoice result = invoiceService.generateMonthlyInvoice(1L);

        // Assert
        assertNotNull(result);
        verify(collectionEventRepository, times(1)).save(testCollection);
        verify(recyclingCollectionRepository, times(1)).save(testRecycling);
    }

    @Test
    void generateMonthlyInvoice_NoCollectionsOrCharges_ShouldThrowException() {
        // Arrange
        testUser.setPendingCharges(0.0);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(collectionEventRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList());
        when(recyclingCollectionRepository.findUninvoicedByResident(1L)).thenReturn(Arrays.asList());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invoiceService.generateMonthlyInvoice(1L);
        });

        assertTrue(exception.getMessage().contains("No collections, recycling, or pending charges"));
    }

    @Test
    void getInvoiceById_ExistingId_ShouldReturnInvoice() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // Act
        Invoice result = invoiceService.getInvoiceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INV-001", result.getInvoiceNumber());
    }

    @Test
    void getInvoiceById_NonExistingId_ShouldThrowException() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invoiceService.getInvoiceById(999L);
        });

        assertTrue(exception.getMessage().contains("Invoice not found"));
    }

    @Test
    void getInvoiceByNumber_ExistingNumber_ShouldReturnInvoice() {
        // Arrange
        when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.of(testInvoice));

        // Act
        Invoice result = invoiceService.getInvoiceByNumber("INV-001");

        // Assert
        assertNotNull(result);
        assertEquals("INV-001", result.getInvoiceNumber());
    }

    @Test
    void markInvoiceAsPaid_ValidInvoice_ShouldUpdateStatus() {
        // Arrange
        when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        // Act
        invoiceService.markInvoiceAsPaid("INV-001", "PAY-123");

        // Assert
        assertEquals(InvoiceStatus.PAID, testInvoice.getStatus());
        assertEquals("ONLINE", testInvoice.getPaymentMethod());
        assertEquals("PAY-123", testInvoice.getPaymentReference());
        verify(invoiceRepository).save(testInvoice);
    }

    @Test
    void processInvoicePayment_ValidInvoice_ShouldProcessPayment() {
        // Arrange
        testInvoice.setFinalAmount(800.0);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());

        // Act
        Invoice result = invoiceService.processInvoicePayment(1L, "CARD", "TXN-123");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(testInvoice);
    }

    @Test
    void processInvoicePayment_AlreadyPaidInvoice_ShouldThrowException() {
        // Arrange
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invoiceService.processInvoicePayment(1L, "CARD", "TXN-123");
        });

        assertTrue(exception.getMessage().contains("already paid"));
    }

    @Test
    void getInvoicesByResident_ValidResident_ShouldReturnInvoices() {
        // Arrange
        when(invoiceRepository.findByResidentId(1L)).thenReturn(Arrays.asList(testInvoice));

        // Act
        List<Invoice> result = invoiceService.getInvoicesByResident(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
    }

    @Test
    void getPendingInvoices_ShouldReturnPendingInvoices() {
        // Arrange
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(Arrays.asList(testInvoice));

        // Act
        List<Invoice> result = invoiceService.getPendingInvoices();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(InvoiceStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void getOverdueInvoices_ShouldReturnOverdueInvoices() {
        // Arrange
        when(invoiceRepository.findByDueDateBeforeAndStatus(any(LocalDate.class), eq(InvoiceStatus.PENDING)))
                .thenReturn(Arrays.asList(testInvoice));

        // Act
        List<Invoice> result = invoiceService.getOverdueInvoices();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void updateInvoiceStatus_ValidStatus_ShouldUpdateInvoice() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        // Act
        Invoice result = invoiceService.updateInvoiceStatus(1L, "PAID");

        // Assert
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).save(testInvoice);
    }

    @Test
    void getTotalRevenueBetween_ValidDates_ShouldReturnRevenue() {
        // Arrange
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        when(invoiceRepository.getTotalRevenueBetween(start, end)).thenReturn(5000.0);

        // Act
        Double result = invoiceService.getTotalRevenueBetween(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(5000.0, result);
    }
}