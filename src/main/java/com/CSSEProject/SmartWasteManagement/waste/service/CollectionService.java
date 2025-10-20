package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollectionService {

    @Autowired
    private CollectionEventRepository collectionEventRepository;

    @Autowired
    private RecyclingCollectionRepository recyclingCollectionRepository;

    @Autowired
    private WasteBinService wasteBinService;

    @Autowired
    private UserService userService;

    @Autowired
    private BillingService billingService;

    // Add these getter methods for other services to access the repositories
    public CollectionEventRepository getCollectionEventRepository() {
        return collectionEventRepository;
    }

    public RecyclingCollectionRepository getRecyclingCollectionRepository() {
        return recyclingCollectionRepository;
    }

    @Transactional
    public CollectionEvent recordCollection(CollectionRequestDto request) {
        // Validate bin and collector
        WasteBin bin = wasteBinService.getBinById(request.getBinId());
        User collector = userService.getUserById(request.getStaffId());

        // Verify collector is staff
        if (collector.getRole() != UserRole.ROLE_STAFF) {
            throw new RuntimeException("Only staff members can record collections");
        }

        // Calculate charge based on billing model
        BillingModel billingModel = billingService.getActiveBillingModelForResident(bin.getResident().getId());
        Double charge = calculateCharge(request.getWeight(), billingModel, request.getWasteType());

        // Create collection record
        CollectionEvent event = new CollectionEvent();
        event.setWasteBin(bin);
        event.setCollector(collector);
        event.setWeight(request.getWeight());
        event.setCalculatedCharge(charge);

        // Update bin level
        wasteBinService.updateBinLevel(bin.getBinId(), 0.0); // Reset to empty after collection

        return collectionEventRepository.save(event);
    }

    @Transactional
    public RecyclingCollection recordRecyclingCollection(RecyclingRequestDto request) {
        // Validate resident and collector
        User resident = userService.getUserById(request.getResidentId());
        User collector = userService.getUserById(request.getStaffId());

        // Verify collector is staff
        if (collector.getRole() != UserRole.ROLE_STAFF) {
            throw new RuntimeException("Only staff members can record recycling collections");
        }

        // Get billing model for payback calculation
        BillingModel billingModel = billingService.getActiveBillingModelForResident(resident.getId());
        Double paybackRate = billingModel.getRecyclingPaybackRates().get(request.getWasteType());

        if (paybackRate == null) {
            throw new RuntimeException("No payback rate defined for waste type: " + request.getWasteType());
        }

        Double paybackAmount = request.getWeight() * paybackRate;

        // Create recycling collection record
        RecyclingCollection recycling = new RecyclingCollection();
        recycling.setResident(resident);
        recycling.setWasteType(request.getWasteType());
        recycling.setWeight(request.getWeight());
        recycling.setPaybackAmount(paybackAmount);

        return recyclingCollectionRepository.save(recycling);
    }

    private Double calculateCharge(Double weight, BillingModel model, BinType wasteType) {
        if (wasteType == BinType.GENERAL_WASTE) {
            switch(model.getBillingType()) {
                case WEIGHT_BASED:
                    return weight * model.getRatePerKg();
                case FLAT_FEE:
                    return model.getMonthlyFlatFee();
                case HYBRID:
                    return model.getBaseFee() + (weight * model.getAdditionalRatePerKg());
                default:
                    return 0.0;
            }
        }
        return 0.0; // Recyclables are not charged
    }

    public List<CollectionEvent> getCollectionsByBin(String binId) {
        return collectionEventRepository.findByWasteBinBinId(binId);
    }

    public List<CollectionEvent> getCollectionsByCollector(Long collectorId) {
        return collectionEventRepository.findByCollectorId(collectorId);
    }

    public Double getTotalWasteCollectedBetween(LocalDateTime start, LocalDateTime end) {
        return collectionEventRepository.getTotalWeightBetween(start, end);
    }

    public Long getCollectionCountBetween(LocalDateTime start, LocalDateTime end) {
        return collectionEventRepository.getCollectionCountBetween(start, end);
    }

    public List<CollectionEvent> getUninvoicedCollections() {
        return collectionEventRepository.findUninvoicedCollections();
    }

    public List<RecyclingCollection> getUninvoicedRecycling() {
        return recyclingCollectionRepository.findUninvoicedRecycling();
    }

    public Double getResidentRecyclingCredits(Long residentId) {
        return recyclingCollectionRepository.getTotalUnusedCreditsByResident(residentId);
    }
}