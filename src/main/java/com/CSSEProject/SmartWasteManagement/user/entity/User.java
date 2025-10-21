package com.CSSEProject.SmartWasteManagement.user.entity;

import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // Resident-specific fields
    private String residentId; // Unique ID from waste management authority
    private LocalDate accountActivationDate;
    @Column(name = "pending_charges")
    private Double pendingCharges = 0.0;

    @Column(name = "total_charges")
    private Double totalCharges = 0.0;

    @Column(name = "recycling_credits")
    private Double recyclingCredits = 0.0;

    // Relationships
    @OneToMany(mappedBy = "resident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore

    private List<WasteBin> wasteBins = new ArrayList<>();

    @OneToMany(mappedBy = "collector", fetch = FetchType.LAZY)
    @JsonIgnore

    private List<com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent> collectionsMade = new ArrayList<>();

    @OneToMany(mappedBy = "resident", fetch = FetchType.LAZY)
    @JsonIgnore

    private List<Invoice> invoices = new ArrayList<>();

    public User() {}
}