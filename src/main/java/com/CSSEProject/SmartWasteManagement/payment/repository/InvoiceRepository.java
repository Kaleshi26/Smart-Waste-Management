package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByResidentId(Long residentId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findByDueDateBeforeAndStatus(LocalDate dueDate, InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.periodStart <= ?1 AND i.periodEnd >= ?1 AND i.resident.id = ?2")
    List<Invoice> findInvoicesForDate(LocalDate date, Long residentId);
    
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'PAID' AND i.periodStart >= ?1 AND i.periodEnd <= ?2")
    Double getTotalRevenueBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = 'PENDING' AND i.dueDate < CURRENT_DATE")
    Long getOverdueInvoiceCount();

}