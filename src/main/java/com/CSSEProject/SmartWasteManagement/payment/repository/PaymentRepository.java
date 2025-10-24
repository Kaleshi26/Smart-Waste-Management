package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.Payment;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
=======
import com.CSSEProject.SmartWasteManagement.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN ?1 AND ?2")
    Double getTotalPaymentsBetween(LocalDateTime start, LocalDateTime end);

    Optional<Payment> findByTransactionId(String transactionId);
}
>>>>>>> main
