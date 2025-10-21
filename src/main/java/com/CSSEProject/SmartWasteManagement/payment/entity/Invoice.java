package com.CSSEProject.SmartWasteManagement.payment.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private Double totalAmount = 0.0;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    @JsonIgnore // Add this to break the cycle

    private User resident;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CollectionEvent> collections = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RecyclingCollection> recyclingCollections = new ArrayList<>();

    public Invoice() {
        this.invoiceDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30); // 30 days from invoice date
    }
}