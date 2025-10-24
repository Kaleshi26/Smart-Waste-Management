package com.CSSEProject.SmartWasteManagement.waste.entity;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "waste_bins")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WasteBin {

    @Id
    @Column(name = "bin_id")
    private String binId;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "bin_type")
    private BinType binType;

    private Double capacity;

    @Column(name = "current_level")
    private Double currentLevel = 0.0;

    @Enumerated(EnumType.STRING)
    private BinStatus status = BinStatus.ACTIVE;

    @Column(name = "rfid_tag")
    private String rfidTag;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    // Option 2: Use @JsonIgnoreProperties with specific fields to exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resident_id")
    @JsonIgnoreProperties({"password", "pendingCharges", "recyclingCredits", "wasteBins"}) // Break circular reference
    private User resident;

    // FIX: Use @JsonIgnore for collections to avoid circular references
    @OneToMany(mappedBy = "wasteBin", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CollectionEvent> collections = new ArrayList<>();

    @OneToMany(mappedBy = "wasteBin", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CollectionSchedule> schedules = new ArrayList<>();
}