// File: src/main/java/com/CSSEProject/SmartWasteManagement/payment/service/PaymentService.java
package com.CSSEProject.SmartWasteManagement.payment.service;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<Invoice> getInvoicesForUser(Long userId) {
        return invoiceRepository.findByUserId(userId);
    }

    public Invoice payInvoice(Long invoiceId) {
        // Find the invoice or throw an error if it doesn't exist
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        // Check if the invoice is already paid
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid.");
        }

        // For this simulation, we'll just mark it as PAID.
        // A real system would have logic to connect to a payment gateway.
        invoice.setStatus(Invoice.InvoiceStatus.PAID);

        // Save the updated invoice back to the database
        return invoiceRepository.save(invoice);
    }
}