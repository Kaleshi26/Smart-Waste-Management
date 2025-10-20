package com.CSSEProject.SmartWasteManagement.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "pricing_plans")
@Data
public class PricingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type")
    private PlanType planType;

    @Column(name = "rate_per_kg", precision = 10, scale = 2)
    private BigDecimal ratePerKg;

    @Column(name = "flat_fee", precision = 10, scale = 2)
    private BigDecimal flatFee;

    @Column(name = "refund_rate", precision = 10, scale = 2)
    private BigDecimal refundRate;

    private boolean active;

    public enum PlanType {
        FLAT_FEE, WEIGHT_BASED
    }
}
