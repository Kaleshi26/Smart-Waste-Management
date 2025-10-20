package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecyclingCollectionRepository extends JpaRepository<RecyclingCollection, Long> {
    List<RecyclingCollection> findByResidentId(Long residentId);
    List<RecyclingCollection> findByWasteType(BinType wasteType);
    List<RecyclingCollection> findByCollectionTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT rc FROM RecyclingCollection rc WHERE rc.invoice IS NULL")
    List<RecyclingCollection> findUninvoicedRecycling();
    
    @Query("SELECT SUM(rc.paybackAmount) FROM RecyclingCollection rc WHERE rc.resident.id = ?1 AND rc.invoice IS NULL")
    Double getTotalUnusedCreditsByResident(Long residentId);
    
    @Query("SELECT SUM(rc.weight) FROM RecyclingCollection rc WHERE rc.collectionTime BETWEEN ?1 AND ?2")
    Double getTotalRecyclingWeightBetween(LocalDateTime start, LocalDateTime end);
}