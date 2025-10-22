package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_events")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CollectionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_time")
    private LocalDateTime collectionTime;

    @Column
    private Double weight;

    @Column(name = "calculated_charge")
    private Double calculatedCharge;

    // FIX: Simple mapping without complex annotations
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bin_id")
    private WasteBin wasteBin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "collector_id")
    private User collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public CollectionEvent() {
        this.collectionTime = LocalDateTime.now();
    }
}