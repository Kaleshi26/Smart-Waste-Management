package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_schedules")
@Data
public class CollectionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", referencedColumnName = "bin_id")
    @JsonBackReference
    private WasteBin wasteBin;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Helper method to get bin ID without loading entire WasteBin
    public String getBinId() {
        return wasteBin != null ? wasteBin.getBinId() : null;
    }

    // Helper method to get resident ID
    public Long getResidentId() {
        return wasteBin != null && wasteBin.getResident() != null ? wasteBin.getResident().getId() : null;
    }

    public CollectionSchedule() {
        this.createdAt = LocalDateTime.now();
    }
}