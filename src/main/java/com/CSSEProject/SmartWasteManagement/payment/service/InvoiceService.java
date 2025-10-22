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
import org.springframework.context.annotation.Lazy;
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
    private BillingService billingService;

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private RecyclingCollectionRepository recyclingCollectionRepository;

    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }

    private Invoice createInvoiceFromPendingCharges(User resident, LocalDate periodStart, LocalDate periodEnd) {
        // Check if resident has any pending charges
        Double pendingCharges = resident.getPendingCharges() != null ? resident.getPendingCharges() : 0.0;
        Double recyclingCredits = resident.getRecyclingCredits() != null ? resident.getRecyclingCredits() : 0.0;

        if (pendingCharges <= 0 && recyclingCredits <= 0) {
            throw new RuntimeException("No collections, recycling, or pending charges to invoice for resident: " + resident.getName());
        }

        // Create minimal invoice from pending charges
        Invoice invoice = new Invoice();
        invoice.setResident(resident);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setBaseCharge(pendingCharges);
        invoice.setWeightBasedCharge(0.0);
        invoice.setRecyclingCredits(recyclingCredits);
        invoice.setTotalAmount(Math.max(0, pendingCharges - recyclingCredits));
        invoice.setStatus(InvoiceStatus.PENDING);

        // Reset resident's charges after invoicing
        resident.setPendingCharges(0.0);
        resident.setRecyclingCredits(0.0);
        userService.updateUser(resident); // You might need to add this method to UserService

        return invoiceRepository.save(invoice);
    }
    @Transactional
    public Invoice generateMonthlyInvoice(Long residentId) {
        User resident = userService.getUserById(residentId);

        // Use current month period
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = LocalDate.now();

        // Get uninvoiced collections
        List<CollectionEvent> collections = collectionEventRepository.findUninvoicedByResident(residentId);
        List<RecyclingCollection> recyclings = recyclingCollectionRepository.findUninvoicedByResident(residentId);

        // Check both collections AND pending charges
        boolean hasCollections = !collections.isEmpty() || !recyclings.isEmpty();
        boolean hasPendingCharges = resident.getPendingCharges() != null && resident.getPendingCharges() > 0;

        System.out.println("🔍 Invoice Generation Debug:");
        System.out.println("   - Resident: " + resident.getName() + " (ID: " + resident.getId() + ")");
        System.out.println("   - Pending Charges: " + resident.getPendingCharges());
        System.out.println("   - Uninvoiced Collections: " + collections.size());
        System.out.println("   - Has Collections: " + hasCollections);
        System.out.println("   - Has Pending Charges: " + hasPendingCharges);

        if (!hasCollections && !hasPendingCharges) {
            throw new RuntimeException("No collections, recycling, or pending charges to invoice for resident: " + resident.getName());
        }

        Double totalCharges = 0.0;
        Double totalCredits = 0.0;

        // Calculate from collections if they exist
        if (hasCollections) {
            totalCharges = collections.stream()
                    .mapToDouble(ce -> ce.getCalculatedCharge() != null ? ce.getCalculatedCharge() : 0.0)
                    .sum();

            totalCredits = recyclings.stream()
                    .mapToDouble(rc -> rc.getPaybackAmount() != null ? rc.getPaybackAmount() : 0.0)
                    .sum();
        }

        // ADD pending charges if they exist
        if (hasPendingCharges) {
            totalCharges += resident.getPendingCharges();
        }

        Double finalAmount = Math.max(0, totalCharges - totalCredits);

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setResident(resident);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setBaseCharge(hasPendingCharges ? resident.getPendingCharges() : 0.0);
        invoice.setWeightBasedCharge(hasCollections ? totalCharges : 0.0);
        invoice.setRecyclingCredits(totalCredits);
        invoice.setTotalAmount(finalAmount);
        invoice.setStatus(InvoiceStatus.PENDING);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Link collections to invoice (if any)
        collections.forEach(ce -> {
            ce.setInvoice(savedInvoice);
            collectionEventRepository.save(ce);
        });

        recyclings.forEach(rc -> {
            rc.setInvoice(savedInvoice);
            recyclingCollectionRepository.save(rc);
        });

        // RESET pending charges after invoicing
        if (hasPendingCharges) {
            resident.setPendingCharges(0.0);
            userService.updateUser(resident);
            System.out.println("✅ Reset pending charges for resident " + resident.getId());
        }

        System.out.println("✅ Invoice generated: " + savedInvoice.getInvoiceNumber() + " Amount: " + savedInvoice.getTotalAmount());
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