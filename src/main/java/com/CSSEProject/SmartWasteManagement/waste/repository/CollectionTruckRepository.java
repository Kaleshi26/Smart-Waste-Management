package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionTruck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionTruckRepository extends JpaRepository<CollectionTruck, Long> {
    Optional<CollectionTruck> findByTruckId(String truckId);
    List<CollectionTruck> findByActiveTrue();
    List<CollectionTruck> findByCurrentDriver(String staffId);
    boolean existsByTruckId(String truckId);
}