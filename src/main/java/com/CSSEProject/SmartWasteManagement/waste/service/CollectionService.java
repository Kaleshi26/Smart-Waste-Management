package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CollectionService {

    @Autowired
    private CollectionEventRepository collectionRepository;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private FeedbackService feedbackService;

    public CollectionEvent recordCollection(CollectionRequestDto request) {
        // 1. Validate bin exists and get details - FIXED: use findById instead of findByBinId
        WasteBin bin = wasteBinRepository.findById(request.getBinId())
                .orElseThrow(() -> {
                    feedbackService.provideErrorFeedback("Bin not found: " + request.getBinId());
                    return new RuntimeException("Bin not found: " + request.getBinId());
                });

        // 2. Get resident for billing
        User resident = bin.getResident();
        if (resident == null) {
            feedbackService.provideErrorFeedback("Bin not assigned to any resident");
            throw new RuntimeException("Bin not assigned to any resident: " + request.getBinId());
        }

        // 3. Validate collection schedule
        if (!isWithinCollectionHours(LocalDateTime.now())) {
            feedbackService.provideErrorFeedback("Collection outside scheduled hours");
            throw new RuntimeException("Collection outside scheduled hours");
        }

        // 4. Get billing model and calculate charges
        BillingModel billingModel = billingService.getActiveBillingModelForResident(resident.getId());
        Double charge = calculateCollectionCharge(billingModel, request.getWeight(), bin.getBinType());

        // 5. Create collection record
        CollectionEvent collection = new CollectionEvent();
        collection.setCollectionTime(LocalDateTime.now());
        collection.setWeight(request.getWeight());
        collection.setCalculatedCharge(charge);
        collection.setWasteBin(bin);
        collection.setCollector(userRepository.findById(request.getCollectorId()).orElse(null));

        CollectionEvent savedCollection = collectionRepository.save(collection);

        // 6. Update resident's pending charges
        updateResidentPendingCharges(resident, charge);

        // 7. Provide feedback
        feedbackService.provideSuccessFeedback("Collection recorded successfully for bin " + request.getBinId());
        feedbackService.provideAudioConfirmation("Collection recorded successfully");

        return savedCollection;
    }

    // Alternative method if you need to lookup by RFID tag
    public CollectionEvent recordCollectionByRfid(CollectionRequestDto request) {
        // Lookup bin by RFID tag instead of binId
        WasteBin bin = wasteBinRepository.findByRfidTag(request.getRfidTag())
                .orElseThrow(() -> {
                    feedbackService.provideErrorFeedback("Bin not found with RFID: " + request.getRfidTag());
                    return new RuntimeException("Bin not found with RFID: " + request.getRfidTag());
                });

        // Continue with the same logic as above...
        return recordCollection(createRequestFromBin(bin, request));
    }

    private CollectionRequestDto createRequestFromBin(WasteBin bin, CollectionRequestDto originalRequest) {
        CollectionRequestDto newRequest = new CollectionRequestDto();
        newRequest.setBinId(bin.getBinId());
        newRequest.setCollectorId(originalRequest.getCollectorId());
        newRequest.setWeight(originalRequest.getWeight());
        newRequest.setTruckId(originalRequest.getTruckId());
        newRequest.setOfflineMode(originalRequest.isOfflineMode());
        newRequest.setDeviceId(originalRequest.getDeviceId());
        return newRequest;
    }

    public CollectionEvent recordRecyclingCollection(RecyclingRequestDto request) {
        // Implementation for recycling collections
        User resident = userRepository.findById(request.getResidentId())
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        // Calculate recycling payback
        BillingModel billingModel = billingService.getActiveBillingModelForResident(resident.getId());
        Double paybackAmount = calculateRecyclingPayback(billingModel, request.getWeight(), request.getWasteType());

        // Update resident recycling credits
        updateResidentRecyclingCredits(resident, paybackAmount);

        // Create recycling collection record (you'll need to implement this entity)
        // RecyclingCollection recycling = new RecyclingCollection();
        // ... set properties
        // return recyclingCollectionRepository.save(recycling);

        return null; // Placeholder
    }

    private Double calculateCollectionCharge(BillingModel model, Double weight, BinType binType) {
        if (model == null) {
            return 0.0; // Default no charge if no billing model
        }

        switch (model.getBillingType()) {
            case WEIGHT_BASED:
                return weight * (model.getRatePerKg() != null ? model.getRatePerKg() : 0.0);
            case FLAT_FEE:
                return model.getMonthlyFlatFee() != null ? model.getMonthlyFlatFee() : 0.0;
            case HYBRID:
                Double base = model.getBaseFee() != null ? model.getBaseFee() : 0.0;
                Double additional = model.getAdditionalRatePerKg() != null ? model.getAdditionalRatePerKg() : 0.0;
                return base + (weight * additional);
            default:
                return 0.0;
        }
    }

    private Double calculateRecyclingPayback(BillingModel model, Double weight, BinType wasteType) {
        if (model == null || model.getRecyclingPaybackRates() == null) {
            return 0.0;
        }
        Double rate = model.getRecyclingPaybackRates().get(wasteType);
        return rate != null ? weight * rate : 0.0;
    }

    private boolean isWithinCollectionHours(LocalDateTime time) {
        // Simple implementation - check if within 6 AM to 6 PM
        int hour = time.getHour();
        return hour >= 6 && hour <= 18;
    }

    private void updateResidentPendingCharges(User resident, Double charge) {
        Double currentCharges = resident.getPendingCharges() != null ? resident.getPendingCharges() : 0.0;
        resident.setPendingCharges(currentCharges + charge);
        userRepository.save(resident);
    }

    private void updateResidentRecyclingCredits(User resident, Double credits) {
        Double currentCredits = resident.getRecyclingCredits() != null ? resident.getRecyclingCredits() : 0.0;
        resident.setRecyclingCredits(currentCredits + credits);
        userRepository.save(resident);
    }

    // Existing methods from your current implementation
    public List<CollectionEvent> getCollectionsByBin(String binId) {
        return collectionRepository.findByWasteBinBinId(binId);
    }

    public List<CollectionEvent> getCollectionsByCollector(Long collectorId) {
        return collectionRepository.findByCollectorId(collectorId);
    }

    public Double getTotalWasteCollectedBetween(LocalDateTime start, LocalDateTime end) {
        return collectionRepository.getTotalWeightBetween(start, end);
    }

    public Long getCollectionCountBetween(LocalDateTime start, LocalDateTime end) {
        return collectionRepository.getCollectionCountBetween(start, end);
    }

    public List<CollectionEvent> getUninvoicedCollections() {
        return collectionRepository.findUninvoicedCollections();
    }

    public Double getResidentRecyclingCredits(Long residentId) {
        User resident = userRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Resident not found"));
        return resident.getRecyclingCredits() != null ? resident.getRecyclingCredits() : 0.0;
    }

    public List<CollectionEvent> getUninvoicedRecycling() {
        // Implement based on your recycling collection entity
        return List.of(); // Placeholder
    }
    // Add these methods to your existing CollectionService class

    // Repository getter methods for InvoiceService
    public CollectionEventRepository getCollectionEventRepository() {
        return collectionRepository;
    }


    public RecyclingCollectionRepository getRecyclingCollectionRepository() {
        // You'll need to autowire this in CollectionService
        // For now, return null or implement properly
        return null;
    }
}