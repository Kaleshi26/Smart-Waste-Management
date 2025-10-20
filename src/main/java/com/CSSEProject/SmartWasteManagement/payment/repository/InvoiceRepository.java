// File: src/main/java/com/CSSEProject/SmartWasteManagement/payment/repository/InvoiceRepository.java
package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Custom method to find all invoices for a specific user by their ID
    List<Invoice> findByUserId(Long userId);
}