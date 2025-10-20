package com.CSSEProject.SmartWasteManagement.user.entity;

import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
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

    // Relationships
    @OneToMany(mappedBy = "resident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WasteBin> wasteBins = new ArrayList<>();

    @OneToMany(mappedBy = "collector", fetch = FetchType.LAZY)
    private List<com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent> collectionsMade = new ArrayList<>();

    @OneToMany(mappedBy = "resident", fetch = FetchType.LAZY)
    private List<Invoice> invoices = new ArrayList<>();

    public User() {}
}