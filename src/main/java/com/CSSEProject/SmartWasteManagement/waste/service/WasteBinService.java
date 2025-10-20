package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class WasteBinService {

    @Autowired
    private WasteBinRepository wasteBinRepository;

    @Autowired
    private UserService userService;

    public WasteBin createWasteBin(WasteBin wasteBin, Long residentId) {
        if (wasteBinRepository.existsByBinId(wasteBin.getBinId())) {
            throw new RuntimeException("Bin with ID " + wasteBin.getBinId() + " already exists");
        }

        if (residentId != null) {
            User resident = userService.getUserById(residentId);
            if (resident.getRole() != com.CSSEProject.SmartWasteManagement.user.entity.UserRole.ROLE_RESIDENT) {
                throw new RuntimeException("Only residents can be assigned waste bins");
            }
            wasteBin.setResident(resident);
        }

        wasteBin.setInstallationDate(LocalDate.now());
        wasteBin.setStatus(BinStatus.ACTIVE);
        
        return wasteBinRepository.save(wasteBin);
    }

    public WasteBin getBinById(String binId) {
        return wasteBinRepository.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found with ID: " + binId));
    }

    public WasteBin getBinByRfid(String rfidTag) {
        return wasteBinRepository.findByRfidTag(rfidTag)
                .orElseThrow(() -> new RuntimeException("Bin not found with RFID: " + rfidTag));
    }

    public List<WasteBin> getBinsByResident(Long residentId) {
        return wasteBinRepository.findByResidentId(residentId);
    }

    public List<WasteBin> getBinsByStatus(BinStatus status) {
        return wasteBinRepository.findByStatus(status);
    }

    public WasteBin updateBinStatus(String binId, BinStatus status) {
        WasteBin bin = getBinById(binId);
        bin.setStatus(status);
        return wasteBinRepository.save(bin);
    }

    public WasteBin updateBinLevel(String binId, Double currentLevel) {
        WasteBin bin = getBinById(binId);
        bin.setCurrentLevel(currentLevel);
        
        // Auto-update status based on level
        if (currentLevel >= 80) {
            bin.setStatus(BinStatus.NEEDS_EMPTYING);
        }
        
        return wasteBinRepository.save(bin);
    }

    public WasteBin assignBinToResident(String binId, Long residentId) {
        WasteBin bin = getBinById(binId);
        User resident = userService.getUserById(residentId);
        
        if (resident.getRole() != com.CSSEProject.SmartWasteManagement.user.entity.UserRole.ROLE_RESIDENT) {
            throw new RuntimeException("Only residents can be assigned waste bins");
        }
        
        bin.setResident(resident);
        return wasteBinRepository.save(bin);
    }

    public long getBinCountByStatus(BinStatus status) {
        return wasteBinRepository.countByStatus(status);
    }
}