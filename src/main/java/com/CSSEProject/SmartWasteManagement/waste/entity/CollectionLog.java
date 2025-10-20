package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_logs")
@Data
public class CollectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collectionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bin_id")
    private String binId;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type")
    private WasteType wasteType;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 10, scale = 2)
    private BigDecimal charge;

    @Column(name = "collection_date")
    private LocalDateTime collectionDate;

    @Enumerated(EnumType.STRING)
    private CollectionStatus status;

    public enum WasteType {
        ORGANIC, PLASTIC, METAL, RECYCLABLE, OTHER
    }

    public enum CollectionStatus {
        RECORDED, SYNCED
    }
}
