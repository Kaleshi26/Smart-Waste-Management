package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WasteBinRepository extends JpaRepository<WasteBin, String> {

    // FIXED: Use the relationship path instead of direct field
    @Query("SELECT wb FROM WasteBin wb WHERE wb.resident.id = :residentId")
    List<WasteBin> findByResidentId(@Param("residentId") Long residentId);

    List<WasteBin> findByStatus(BinStatus status);
    List<WasteBin> findByBinType(BinType binType);
    Optional<WasteBin> findByRfidTag(String rfidTag);
    Optional<WasteBin> findByQrCode(String qrCode);
    boolean existsByBinId(String binId);
    List<WasteBin> findByLocationContaining(String location);
    long countByStatus(BinStatus status);
}