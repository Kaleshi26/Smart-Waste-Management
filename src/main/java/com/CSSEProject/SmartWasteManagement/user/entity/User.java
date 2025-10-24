package com.CSSEProject.SmartWasteManagement.user.entity;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
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
@Table(name = "users")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private String address;
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    // Resident-specific fields
    private String residentId;
    private LocalDate accountActivationDate;
    private Double pendingCharges = 0.0;
    private Double totalCharges = 0.0;
    private Double recyclingCredits = 0.0;

    // In User entity
    @OneToMany(mappedBy = "resident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"resident", "collections", "schedules"}) // Break circular reference
    private List<WasteBin> wasteBins = new ArrayList<>();

    // FIX: Use @JsonIgnore for collections to avoid circular references
    @OneToMany(mappedBy = "collector", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CollectionEvent> collectionsMade = new ArrayList<>();

    @OneToMany(mappedBy = "resident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Invoice> invoices = new ArrayList<>();

    // Helper methods
    public void addWasteBin(WasteBin bin) {
        wasteBins.add(bin);
        bin.setResident(this);
    }

    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setResident(this);
    }
}