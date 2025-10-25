package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
<<<<<<< HEAD
    // Custom method to find all invoices for a specific user by their ID
    List<Invoice> findByUserId(Long userId);
=======
    List<Invoice> findByResidentId(Long residentId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findByDueDateBeforeAndStatus(LocalDate dueDate, InvoiceStatus status);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    @Query("SELECT i FROM Invoice i WHERE i.periodStart <= ?1 AND i.periodEnd >= ?1 AND i.resident.id = ?2")
    List<Invoice> findInvoicesForDate(LocalDate date, Long residentId);
    
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'PAID' AND i.periodStart >= ?1 AND i.periodEnd <= ?2")
    Double getTotalRevenueBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = 'PENDING' AND i.dueDate < CURRENT_DATE")
    Long getOverdueInvoiceCount();

<<<<<<< HEAD
>>>>>>> main
}
=======
    // Add this method for admin view
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.resident ORDER BY i.invoiceDate DESC")
    List<Invoice> findAllWithResident();}
>>>>>>> main
