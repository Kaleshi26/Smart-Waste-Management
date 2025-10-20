package com.CSSEProject.SmartWasteManagement.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "billing_cycles")
@Data
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_invoices")
    private int totalInvoices;

    @Column(name = "total_revenue", precision = 10, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_refunds", precision = 10, scale = 2)
    private BigDecimal totalRefunds;

    @Enumerated(EnumType.STRING)
    private CycleStatus status;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invoice> invoices;

    public enum CycleStatus {
        OPEN, CLOSED
    }
}
