package com.CSSEProject.SmartWasteManagement.payment.service;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.entity.Payment;
import com.CSSEProject.SmartWasteManagement.payment.entity.PaymentStatus;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import com.CSSEProject.SmartWasteManagement.payment.repository.PaymentRepository;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private RecyclingCollectionRepository recyclingCollectionRepository;

    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }

    @Transactional
    public Invoice generateMonthlyInvoice(Long residentId) {
        User resident = userService.getUserById(residentId);

        // Define billing period (previous month)
        LocalDate periodEnd = LocalDate.now().withDayOfMonth(1).minusDays(1); // Last day of previous month
        LocalDate periodStart = periodEnd.withDayOfMonth(1); // First day of previous month

        // Check if invoice already exists for this period
        List<Invoice> existingInvoices = invoiceRepository.findInvoicesForDate(periodStart, residentId);
        if (!existingInvoices.isEmpty()) {
            throw new RuntimeException("Invoice already exists for period: " + periodStart + " to " + periodEnd);
        }

        // Get uninvoiced collections for this resident
        List<CollectionEvent> collections = collectionService.getUninvoicedCollections().stream()
                .filter(ce -> ce.getWasteBin() != null &&
                        ce.getWasteBin().getResident() != null &&
                        ce.getWasteBin().getResident().getId().equals(residentId))
                .toList();

        // Get uninvoiced recycling collections for this resident
        List<RecyclingCollection> recyclings = getUninvoicedRecyclingCollections().stream()
                .filter(rc -> rc.getResident() != null && rc.getResident().getId().equals(residentId))
                .toList();

        // Calculate charges and credits
        Double totalCharges = collections.stream()
                .mapToDouble(ce -> ce.getCalculatedCharge() != null ? ce.getCalculatedCharge() : 0.0)
                .sum();

        Double totalCredits = recyclings.stream()
                .mapToDouble(rc -> rc.getPaybackAmount() != null ? rc.getPaybackAmount() : 0.0)
                .sum();

        Double finalAmount = Math.max(0, totalCharges - totalCredits); // Ensure non-negative

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setResident(resident);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30)); // Due in 30 days
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setBaseCharge(0.0); // Could be base fee from billing model
        invoice.setWeightBasedCharge(totalCharges);
        invoice.setRecyclingCredits(totalCredits);
        invoice.setTotalAmount(finalAmount);
        invoice.setStatus(InvoiceStatus.PENDING);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Link collections to invoice
        collections.forEach(ce -> {
            ce.setInvoice(savedInvoice);
            collectionEventRepository.save(ce);
        });

        // Link recycling collections to invoice
        recyclings.forEach(rc -> {
            rc.setInvoice(savedInvoice);
            recyclingCollectionRepository.save(rc);
        });

        return savedInvoice;
    }

    @Transactional
    public Payment processInvoicePayment(Long invoiceId, String paymentMethod, String transactionId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid");
        }

        if (invoice.getTotalAmount() <= 0) {
            throw new RuntimeException("Invoice has no amount due");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setUser(invoice.getResident());
        payment.setAmount(invoice.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setStatus(PaymentStatus.COMPLETED);

        // Update invoice status
        invoice.setStatus(InvoiceStatus.PAID);

        invoiceRepository.save(invoice);
        Payment savedPayment = paymentRepository.save(payment);

        return savedPayment;
    }

    public List<Invoice> getInvoicesByResident(Long residentId) {
        return invoiceRepository.findByResidentId(residentId);
    }

    public List<Invoice> getPendingInvoices() {
        return invoiceRepository.findByStatus(InvoiceStatus.PENDING);
    }

    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findByDueDateBeforeAndStatus(LocalDate.now(), InvoiceStatus.PENDING);
    }

    public Double getTotalRevenueBetween(LocalDate start, LocalDate end) {
        return invoiceRepository.getTotalRevenueBetween(start, end);
    }

    // Helper method to get uninvoiced recycling collections
    private List<RecyclingCollection> getUninvoicedRecyclingCollections() {
        // If you have a repository method for this, use it
        // For now, return empty list or implement based on your structure
        return recyclingCollectionRepository.findByInvoiceIsNull();
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }
}