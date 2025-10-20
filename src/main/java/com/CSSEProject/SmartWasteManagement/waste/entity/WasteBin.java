package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "waste_bins")
@Data
public class WasteBin {

    @Id
    @Column(name = "bin_id")
    private String binId; // RFID/QR code from physical device

    @Column(nullable = false)
    private String location; // GPS coordinates or address

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinType binType;

    private Double capacity; // in liters
    private Double currentLevel; // 0-100% from sensors

    @Enumerated(EnumType.STRING)
    private BinStatus status = BinStatus.ACTIVE;

    // Digital tracking
    private String rfidTag;
    private String qrCode;
    private LocalDate installationDate;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    @JsonIgnore // Add this to break the cycle

    private User resident;

    @OneToMany(mappedBy = "wasteBin", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // ← ADD THIS LINE to break the cycle

    private List<CollectionEvent> collections = new ArrayList<>();

    public WasteBin() {}
}