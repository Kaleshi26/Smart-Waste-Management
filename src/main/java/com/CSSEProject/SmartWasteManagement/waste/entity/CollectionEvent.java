// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/waste/entity/CollectionEvent.java
package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/waste/entity/CollectionEvent.java
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

    // NEW: Recycling fields
    @Column(name = "recyclable_weight")
    private Double recyclableWeight = 0.0;

    @Column(name = "refund_amount")
    private Double refundAmount = 0.0;

    @Column(name = "recyclable_items_count")
    private Integer recyclableItemsCount = 0;

    // Relationships
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bin_id")
    private WasteBin wasteBin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "collector_id")
    private User collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @JsonIgnore // ADD THIS
    private Invoice invoice;

    // NEW: One collection can have multiple recycling records
    @OneToMany(mappedBy = "collectionEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // ADD THIS to prevent circular reference
    private List<RecyclingCollection> recyclingCollections = new ArrayList<>();

    public CollectionEvent() {
        this.collectionTime = LocalDateTime.now();
    }

    // Helper method to add recycling collection
    public void addRecyclingCollection(RecyclingCollection recycling) {
        this.recyclingCollections.add(recycling);
        recycling.setCollectionEvent(this);

        // Update summary fields
        this.recyclableWeight = this.recyclingCollections.stream()
                .mapToDouble(RecyclingCollection::getWeight)
                .sum();
        this.refundAmount = this.recyclingCollections.stream()
                .mapToDouble(RecyclingCollection::getPaybackAmount)
                .sum();
        this.recyclableItemsCount = this.recyclingCollections.size();
    }
}