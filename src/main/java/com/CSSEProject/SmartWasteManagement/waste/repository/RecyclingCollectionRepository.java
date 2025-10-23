// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/waste/repository/RecyclingCollectionRepository.java
package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecyclingCollectionRepository extends JpaRepository<RecyclingCollection, Long> {

    // Add this method if missing
    @Query("SELECT rc FROM RecyclingCollection rc WHERE rc.invoice IS NULL AND rc.resident.id = :residentId")
    List<RecyclingCollection> findUninvoicedByResident(@Param("residentId") Long residentId);

    // Existing method
    List<RecyclingCollection> findByInvoiceIsNull();
}