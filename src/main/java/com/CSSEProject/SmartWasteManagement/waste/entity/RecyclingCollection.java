// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/waste/entity/RecyclingCollection.java
package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/waste/entity/RecyclingCollection.java
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
    private RecyclableType recyclableType;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Double paybackAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QualityGrade quality;

    @Column
    private String notes;

    // Relationships - ADD @JsonIgnore to prevent circular references
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    @JsonIgnore
    private User resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_event_id")
    @JsonIgnore
    private CollectionEvent collectionEvent;

    public RecyclingCollection() {
        this.collectionTime = LocalDateTime.now();
    }
}