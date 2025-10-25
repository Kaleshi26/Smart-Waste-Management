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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private RecyclingCollectionRepository recyclingCollectionRepository;

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
        System.out.println("   - Uninvoiced Recycling: " + recyclings.size());
        System.out.println("   - Has Collections: " + hasCollections);
        System.out.println("   - Has Pending Charges: " + hasPendingCharges);

        if (!hasCollections && !hasPendingCharges) {
            throw new RuntimeException("No collections, recycling, or pending charges to invoice for resident: " + resident.getName());
        }

        Double totalCharges = 0.0;
        Double totalCredits = 0.0;
        Double totalRefunds = 0.0;
        Double totalRecyclableWeight = 0.0;

        // Calculate from collections if they exist
        if (hasCollections) {
            totalCharges = collections.stream()
                    .mapToDouble(ce -> ce.getCalculatedCharge() != null ? ce.getCalculatedCharge() : 0.0)
                    .sum();

            totalRefunds = recyclings.stream()
                    .mapToDouble(rc -> rc.getPaybackAmount() != null ? rc.getPaybackAmount() : 0.0)
                    .sum();

            totalRecyclableWeight = recyclings.stream()
                    .mapToDouble(rc -> rc.getWeight() != null ? rc.getWeight() : 0.0)
                    .sum();
        }

        // ADD pending charges if they exist
        if (hasPendingCharges) {
            totalCharges += resident.getPendingCharges();
        }

        Double finalAmount = Math.max(0, totalCharges - totalRefunds);

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
        invoice.setRecyclingCredits(totalRefunds);
        invoice.setRefundAmount(totalRefunds);
        invoice.setRecyclableWeight(totalRecyclableWeight);
        invoice.setTotalAmount(totalCharges); // Total before refunds
        invoice.setFinalAmount(finalAmount);  // Final after refunds
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

        System.out.println("✅ Invoice generated: " + savedInvoice.getInvoiceNumber());
        System.out.println("   - Total Charges: Rs." + savedInvoice.getTotalAmount());
        System.out.println("   - Refunds: Rs." + savedInvoice.getRefundAmount());
        System.out.println("   - Final Amount: Rs." + savedInvoice.getFinalAmount());

        return savedInvoice;
    }
    // Add to your existing InvoiceService

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllWithResident(); // You'll need this method in repository
    }

    public Invoice updateInvoiceStatus(Long invoiceId, String status) {
        Invoice invoice = getInvoiceById(invoiceId);
        invoice.setStatus(InvoiceStatus.valueOf(status));
        return invoiceRepository.save(invoice);
    }

    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }

    // 🆕 NEW METHOD: Get invoice by invoice number (for PayHere integration)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
    }

    // 🆕 NEW METHOD: Mark invoice as paid after PayHere payment
    public void markInvoiceAsPaid(String invoiceNumber, String paymentId) {
        try {
            Invoice invoice = getInvoiceByNumber(invoiceNumber);

            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDate.now());
            invoice.setPaymentMethod("ONLINE");
            invoice.setPaymentReference(paymentId);

            invoiceRepository.save(invoice);

            System.out.println("✅ Invoice marked as PAID: " + invoiceNumber);
            System.out.println("   - Payment Reference: " + paymentId);
            System.out.println("   - Payment Date: " + LocalDate.now());

        } catch (Exception e) {
            System.err.println("❌ Error marking invoice as paid: " + e.getMessage());
            throw new RuntimeException("Failed to update invoice status: " + e.getMessage());
        }
    }

    @Transactional
    public Invoice processInvoicePayment(Long invoiceId, String paymentMethod, String transactionId) {
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
        paymentRepository.save(payment);

        return invoice;
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