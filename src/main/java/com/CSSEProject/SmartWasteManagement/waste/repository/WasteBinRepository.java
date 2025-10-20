package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WasteBinRepository extends JpaRepository<WasteBin, String> {
    List<WasteBin> findByResidentId(Long residentId);
    List<WasteBin> findByStatus(BinStatus status);
    List<WasteBin> findByBinType(BinType binType);
    Optional<WasteBin> findByRfidTag(String rfidTag);
    Optional<WasteBin> findByQrCode(String qrCode);
    boolean existsByBinId(String binId);
    List<WasteBin> findByLocationContaining(String location);
    long countByStatus(BinStatus status);
}