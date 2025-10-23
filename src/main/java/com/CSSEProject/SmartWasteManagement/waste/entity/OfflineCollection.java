package com.CSSEProject.SmartWasteManagement.waste.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "offline_collections")
@Data
public class OfflineCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String binId;

    @Column(nullable = false)
    private Long collectorId;

    private String rfidTag;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private LocalDateTime collectionTime;

    @Column(nullable = false)
    private String deviceId;

    private Boolean synced = false;

    private LocalDateTime syncTime;

    @Column(columnDefinition = "TEXT")
    private String collectionData;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OfflineCollection() {
        this.collectionTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}