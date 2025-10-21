package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecyclingCollectionRepository extends JpaRepository<RecyclingCollection, Long> {
    List<RecyclingCollection> findByInvoiceIsNull();
    List<RecyclingCollection> findByResidentId(Long residentId);
    List<RecyclingCollection> findByCollectionTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}