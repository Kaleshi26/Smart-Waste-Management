package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
}
