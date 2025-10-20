package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "recycling_collections")
@Data
public class RecyclingCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime collectionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinType wasteType;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Double paybackAmount;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private User resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public RecyclingCollection() {
        this.collectionTime = LocalDateTime.now();
    }
}