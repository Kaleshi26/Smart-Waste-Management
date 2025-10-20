package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollectionEventRepository extends JpaRepository<CollectionEvent, Long> {

    // FIXED: Use the relationship instead of direct binId
    @Query("SELECT ce FROM CollectionEvent ce WHERE ce.wasteBin.binId = ?1")
    List<CollectionEvent> findByWasteBinBinId(String binId);

    List<CollectionEvent> findByCollectorId(Long collectorId);
    List<CollectionEvent> findByCollectionTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ce FROM CollectionEvent ce WHERE ce.invoice IS NULL")
    List<CollectionEvent> findUninvoicedCollections();

    @Query("SELECT SUM(ce.weight) FROM CollectionEvent ce WHERE ce.collectionTime BETWEEN ?1 AND ?2")
    Double getTotalWeightBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(ce) FROM CollectionEvent ce WHERE ce.collectionTime BETWEEN ?1 AND ?2")
    Long getCollectionCountBetween(LocalDateTime start, LocalDateTime end);
}