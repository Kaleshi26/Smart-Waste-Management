package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollectionEventRepository extends JpaRepository<CollectionEvent, Long> {

    // FIXED: Use proper relationship path
    @Query("SELECT c FROM CollectionEvent c " +
            "LEFT JOIN FETCH c.wasteBin wb " +
            "LEFT JOIN FETCH wb.resident " +
            "WHERE c.collector.id = :collectorId " +
            "ORDER BY c.collectionTime DESC")
    List<CollectionEvent> findByCollectorId(@Param("collectorId") Long collectorId);


    // Existing methods...
    @Query("SELECT c FROM CollectionEvent c WHERE c.wasteBin.binId = :binId")
    List<CollectionEvent> findByWasteBinBinId(@Param("binId") String binId);

    List<CollectionEvent> findByCollectionTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM CollectionEvent c WHERE c.invoice IS NULL")
    List<CollectionEvent> findUninvoicedCollections();

    @Query("SELECT SUM(c.weight) FROM CollectionEvent c WHERE c.collectionTime BETWEEN :start AND :end")
    Double getTotalWeightBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM CollectionEvent c WHERE c.collectionTime BETWEEN :start AND :end")
    Long getCollectionCountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM CollectionEvent c WHERE c.wasteBin.resident.id = :residentId AND c.invoice IS NULL")
    List<CollectionEvent> findUninvoicedByResident(@Param("residentId") Long residentId);
}