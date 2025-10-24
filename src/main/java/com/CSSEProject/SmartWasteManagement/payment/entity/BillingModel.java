package com.CSSEProject.SmartWasteManagement.payment.entity;

import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import jakarta.persistence.*;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "billing_models")
@Data
public class BillingModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingType billingType;

    // Weight-based pricing
    private Double ratePerKg;

    // Flat fee pricing  
    private Double monthlyFlatFee;

    // Hybrid model
    private Double baseFee;
    private Double additionalRatePerKg;

    // Recycling payback rates
    @ElementCollection
    @CollectionTable(name = "recycling_payback_rates", 
                    joinColumns = @JoinColumn(name = "billing_model_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "waste_type")
    @Column(name = "payback_rate")
    private Map<BinType, Double> recyclingPaybackRates = new HashMap<>();

    @Column(nullable = false)
    private Boolean active = true;

    public BillingModel() {}
}