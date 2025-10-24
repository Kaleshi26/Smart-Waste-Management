package com.CSSEProject.SmartWasteManagement.payment.repository;

import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingModelRepository extends JpaRepository<BillingModel, Long> {
    List<BillingModel> findByCity(String city);
    Optional<BillingModel> findByCityAndActiveTrue(String city);
    List<BillingModel> findByActiveTrue();
    boolean existsByCityAndActiveTrue(String city);

}