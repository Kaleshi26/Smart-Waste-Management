package com.CSSEProject.SmartWasteManagement.payment.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
<<<<<<< HEAD
import java.time.LocalDateTime;
=======
import java.util.ArrayList;
import java.util.List;
>>>>>>> main

@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

<<<<<<< HEAD
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = false)
    private BillingCycle cycle;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    public enum InvoiceStatus {
        PENDING, PAID, FAILED
=======
    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    // Billing period
    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    // Charges
    @Column(nullable = false)
    private Double baseCharge = 0.0;

    @Column(nullable = false)
    private Double weightBasedCharge = 0.0;

    @Column(nullable = false)
    private Double recyclingCredits = 0.0;

    // Refund fields for recycling
    @Column(nullable = false)
    private Double refundAmount = 0.0;

    @Column(nullable = false)
    private Double recyclableWeight = 0.0;

    @Column(nullable = false)
    private Double totalAmount = 0.0;

    // Final amount after refunds
    @Column(nullable = false)
    private Double finalAmount = 0.0;

    // 🆕 NEW PAYMENT FIELDS
    private LocalDate paymentDate;

    private String paymentMethod;

    private String paymentReference;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    @JsonIgnore
    private User resident;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CollectionEvent> collections = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RecyclingCollection> recyclingCollections = new ArrayList<>();

    public Invoice() {
        this.invoiceDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
    }

    // FIXED: Better calculation logic
    @PrePersist
    @PreUpdate
    public void calculateFinalAmount() {
        // If totalAmount is not set, calculate it from components
        if (this.totalAmount == null || this.totalAmount == 0.0) {
            this.totalAmount = (this.baseCharge != null ? this.baseCharge : 0.0)
                    + (this.weightBasedCharge != null ? this.weightBasedCharge : 0.0);
        }

        // Calculate final amount (never negative)
        double total = this.totalAmount != null ? this.totalAmount : 0.0;
        double refund = this.refundAmount != null ? this.refundAmount : 0.0;
        this.finalAmount = Math.max(0.0, total - refund);

        System.out.println("🧮 Invoice Final Calculation:");
        System.out.println("   - Base: " + this.baseCharge);
        System.out.println("   - Weight Based: " + this.weightBasedCharge);
        System.out.println("   - Total: " + this.totalAmount);
        System.out.println("   - Refund: " + this.refundAmount);
        System.out.println("   - Final: " + this.finalAmount);
    }

    // Helper method to manually set amounts with validation
    public void setAmounts(Double totalCharge, Double refund) {
        this.totalAmount = totalCharge != null ? totalCharge : 0.0;
        this.refundAmount = refund != null ? refund : 0.0;
        this.finalAmount = Math.max(0.0, this.totalAmount - this.refundAmount);
        this.recyclingCredits = this.refundAmount; // Sync recycling credits with refund
>>>>>>> main
    }
}