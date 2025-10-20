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

        // Get uninvoiced collections and recycling
        List<CollectionEvent> collections = collectionService.getUninvoicedCollections().stream()
                .filter(ce -> ce.getWasteBin().getResident().getId().equals(residentId))
                .toList();

        List<RecyclingCollection> recyclings = collectionService.getUninvoicedRecycling().stream()
                .filter(rc -> rc.getResident().getId().equals(residentId))
                .toList();

        // Calculate charges and credits
        Double totalCharges = collections.stream()
                .mapToDouble(CollectionEvent::getCalculatedCharge)
                .sum();

        Double totalCredits = recyclings.stream()
                .mapToDouble(RecyclingCollection::getPaybackAmount)
                .sum();

        Double finalAmount = Math.max(0, totalCharges - totalCredits); // Ensure non-negative

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setResident(resident);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setBaseCharge(0.0); // Could be base fee from billing model
        invoice.setWeightBasedCharge(totalCharges);
        invoice.setRecyclingCredits(totalCredits);
        invoice.setTotalAmount(finalAmount);
        invoice.setStatus(InvoiceStatus.PENDING);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Link collections and recycling to invoice - FIXED: Use repositories directly
        collections.forEach(ce -> {
            ce.setInvoice(savedInvoice);
            collectionService.getCollectionEventRepository().save(ce);
        });

        recyclings.forEach(rc -> {
            rc.setInvoice(savedInvoice);
            collectionService.getRecyclingCollectionRepository().save(rc);
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
        payment.setStatus(com.CSSEProject.SmartWasteManagement.payment.entity.PaymentStatus.COMPLETED);

        // Update invoice status
        invoice.setStatus(InvoiceStatus.PAID);

        invoiceRepository.save(invoice);
        paymentRepository.save(payment); // Now we save to payment repository

        return payment;
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

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }
}