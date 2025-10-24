package com.CSSEProject.SmartWasteManagement.waste.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_trucks")
@Data
public class CollectionTruck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String truckId;

    private String currentLocation;

    private String assignedRoute;

    private String currentDriver;

    private Boolean active = true;

    private LocalDateTime lastLocationUpdate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CollectionTruck() {
        this.createdAt = LocalDateTime.now();
        this.lastLocationUpdate = LocalDateTime.now();
    }
}