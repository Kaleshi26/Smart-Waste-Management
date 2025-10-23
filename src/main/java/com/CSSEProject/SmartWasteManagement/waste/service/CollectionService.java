package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.CollectionResponseDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclableItemDto;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.entity.*;
import com.CSSEProject.SmartWasteManagement.waste.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private CollectionScheduleRepository collectionScheduleRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private RecyclingCollectionRepository recyclingCollectionRepository;

    @Autowired
    private InvoiceService invoiceService;

    private final Map<QualityGrade, Double> qualityRefundRates = Map.of(
            QualityGrade.EXCELLENT, 8.0,  // Rs. 8/kg for excellent quality
            QualityGrade.GOOD, 6.0,       // Rs. 6/kg for good quality
            QualityGrade.AVERAGE, 4.0,    // Rs. 4/kg for average quality
            QualityGrade.POOR, 2.0        // Rs. 2/kg for poor quality
    );

    // FIXED: Enhanced method with proper error handling
    public List<CollectionEvent> getCollectionsByCollector(Long collectorId) {
        try {
            System.out.println("🔍 Fetching collections for collector: " + collectorId);
            List<CollectionEvent> collections = collectionRepository.findByCollectorId(collectorId);

            // Log collection details for debugging
            collections.forEach(collection -> {
                System.out.println("📦 Collection ID: " + collection.getId());
                System.out.println("   - Bin: " + (collection.getWasteBin() != null ? collection.getWasteBin().getBinId() : "NULL"));
                System.out.println("   - Location: " + (collection.getWasteBin() != null ? collection.getWasteBin().getLocation() : "NULL"));
                System.out.println("   - Resident: " + (collection.getWasteBin() != null && collection.getWasteBin().getResident() != null ?
                        collection.getWasteBin().getResident().getName() : "NULL"));
            });

            return collections;
        } catch (Exception e) {
            System.err.println("❌ Error fetching collections for collector " + collectorId + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch collections: " + e.getMessage());
        }
    }

    private String buildCollectionFeedback(CollectionRequestDto request, CollectionEvent collection) {
        StringBuilder feedback = new StringBuilder();
        feedback.append("Collection recorded for bin ").append(request.getBinId());

        if (request.hasRecyclables()) {
            feedback.append(" with ")
                    .append(collection.getRecyclableItemsCount())
                    .append(" recyclable items (Refund: Rs.")
                    .append(String.format("%.2f", collection.getRefundAmount()))
                    .append(")");
        }

        return feedback.toString();
    }

    // ENHANCED: Get collections as DTOs with recycling data
    public List<CollectionResponseDto> getCollectionsByCollectorAsDto(Long collectorId) {
        List<CollectionEvent> collections = getCollectionsByCollector(collectorId);
        return collections.stream()
                .map(collection -> {
                    CollectionResponseDto dto = new CollectionResponseDto(collection);
                    // Populate recycling fields
                    dto.setRecyclableWeight(collection.getRecyclableWeight());
                    dto.setRefundAmount(collection.getRefundAmount());
                    dto.setRecyclableItemsCount(collection.getRecyclableItemsCount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<CollectionEvent> getTodayCollectionsByCollector(Long collectorId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        return collectionRepository.findByCollectorId(collectorId).stream()
                .filter(collection -> collection.getCollectionTime() != null &&
                        !collection.getCollectionTime().isBefore(startOfDay) &&
                        !collection.getCollectionTime().isAfter(endOfDay))
                .collect(Collectors.toList());
    }

    // In CollectionService.java - Fix the invoice generation
    private Invoice generateInvoiceAfterCollection(User resident, Double charge, Double weight,
                                                   BinType binType, Double refundAmount, Double recyclableWeight) {
        try {
            System.out.println("🧾 AUTO-GENERATING INVOICE WITH REFUNDS");
            System.out.println("   - Resident: " + resident.getName());
            System.out.println("   - Charge: Rs." + charge);
            System.out.println("   - Refund: Rs." + refundAmount);

            // Calculate final amount properly
            Double finalAmount = Math.max(0.0, charge - (refundAmount != null ? refundAmount : 0.0));
            System.out.println("   - Final: Rs." + finalAmount);

            // Create invoice with refund support
            Invoice invoice = new Invoice();
            invoice.setResident(resident);
            invoice.setInvoiceNumber("INV-AUTO-" + System.currentTimeMillis());
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setDueDate(LocalDate.now().plusDays(30));
            invoice.setPeriodStart(LocalDate.now());
            invoice.setPeriodEnd(LocalDate.now());
            invoice.setBaseCharge(0.0);
            invoice.setWeightBasedCharge(charge);
            invoice.setRecyclingCredits(refundAmount != null ? refundAmount : 0.0);
            invoice.setRefundAmount(refundAmount != null ? refundAmount : 0.0);
            invoice.setRecyclableWeight(recyclableWeight != null ? recyclableWeight : 0.0);

            // FIXED: Set totalAmount to the charge (before refunds)
            invoice.setTotalAmount(charge);

            // FIXED: Set finalAmount to charge - refund
            invoice.setFinalAmount(finalAmount);

            invoice.setStatus(InvoiceStatus.PENDING);

            // Manually trigger calculation to ensure it's correct
            invoice.calculateFinalAmount();

            Invoice savedInvoice = invoiceRepository.save(invoice);

            System.out.println("✅ AUTO-INVOICE GENERATED: " + savedInvoice.getInvoiceNumber());
            System.out.println("   - Total Amount: Rs." + savedInvoice.getTotalAmount());
            System.out.println("   - Refund Applied: Rs." + savedInvoice.getRefundAmount());
            System.out.println("   - Final Amount: Rs." + savedInvoice.getFinalAmount());

            return savedInvoice;

        } catch (Exception e) {
            System.err.println("❌ AUTO-INVOICE FAILED: " + e.getMessage());
            return null;
        }
    }

    // NEW: Link recycling collections to invoice
    private void linkRecyclingToInvoice(List<RecyclingCollection> recyclingCollections, Invoice invoice) {
        for (RecyclingCollection recycling : recyclingCollections) {
            recycling.setInvoice(invoice);
            recyclingCollectionRepository.save(recycling);
        }
    }

    public CollectionEvent recordCollection(CollectionRequestDto request) {
        // 1. Validate bin exists and get details
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
        LocalDateTime collectionTime = LocalDateTime.now();
        if (!isCollectionScheduledForToday(bin.getBinId(), collectionTime.toLocalDate())) {
            feedbackService.provideErrorFeedback("No collection scheduled for this bin today");
            throw new RuntimeException("No collection scheduled for bin: " + request.getBinId() + " on " + collectionTime.toLocalDate());
        }

        // 4. Get billing model and calculate charges
        BillingModel billingModel = billingService.getActiveBillingModelForResident(resident.getId());
        Double charge = calculateCollectionCharge(billingModel, request.getWeight(), bin.getBinType());

        // 5. Create collection record
        CollectionEvent collection = new CollectionEvent();
        collection.setCollectionTime(collectionTime);
        collection.setWeight(request.getWeight());
        collection.setCalculatedCharge(charge);
        collection.setWasteBin(bin);
        collection.setCollector(userRepository.findById(request.getCollectorId()).orElse(null));

        // 6. ✅ PROCESS RECYCLABLES IF ANY
        if (request.hasRecyclables()) {
            processRecyclables(collection, request.getRecyclables(), resident);

            // Update request with calculated values for response
            request.setTotalRecyclableWeight(collection.getRecyclableWeight());
            request.setTotalRefundAmount(collection.getRefundAmount());
        }

        CollectionEvent savedCollection = collectionRepository.save(collection);

        // 7. Update collection schedule status
        updateCollectionScheduleStatus(bin.getBinId(), collectionTime.toLocalDate());

        // 8. ✅ AUTO-GENERATE INVOICE IMMEDIATELY AFTER COLLECTION (with refunds)
        Invoice autoInvoice = generateInvoiceAfterCollection(
                resident, charge, request.getWeight(),
                bin.getBinType(), collection.getRefundAmount(), collection.getRecyclableWeight()
        );

        // Link this collection to the auto-generated invoice
        savedCollection.setInvoice(autoInvoice);

        // Also link recycling collections to invoice
        if (request.hasRecyclables()) {
            linkRecyclingToInvoice(savedCollection.getRecyclingCollections(), autoInvoice);
        }

        collectionRepository.save(savedCollection);

        // 9. Update bin level (reset to 0 after collection)
        updateBinLevelAfterCollection(bin);

        // 10. Update resident recycling credits
        if (request.hasRecyclables()) {
            updateResidentRecyclingCredits(resident, collection.getRefundAmount());
        }

        // 11. Provide feedback
        String feedbackMessage = buildCollectionFeedback(request, collection);
        feedbackService.provideSuccessFeedback(feedbackMessage);
        feedbackService.provideAudioConfirmation("Collection recorded successfully");

        return savedCollection;
    }

    private void processRecyclables(CollectionEvent collection, List<RecyclableItemDto> recyclables, User resident) {
        for (RecyclableItemDto recyclable : recyclables) {
            // Validate recyclable
            if (recyclable.getWeightKg() == null || recyclable.getWeightKg() <= 0) {
                throw new RuntimeException("Invalid weight for recyclable: " + recyclable.getType());
            }

            if (recyclable.getQuality() == null) {
                recyclable.setQuality(QualityGrade.AVERAGE); // Default quality
            }

            // Calculate payback amount based on quality
            Double paybackAmount = calculateRecyclingPayback(recyclable);

            // Create recycling collection record
            RecyclingCollection recycling = new RecyclingCollection();
            recycling.setCollectionTime(collection.getCollectionTime());
            recycling.setRecyclableType(recyclable.getType());
            recycling.setWeight(recyclable.getWeightKg());
            recycling.setPaybackAmount(paybackAmount);
            recycling.setQuality(recyclable.getQuality());
            recycling.setNotes(recyclable.getNotes());
            recycling.setResident(resident);
            recycling.setCollectionEvent(collection);

            // Add to collection
            collection.addRecyclingCollection(recycling);
        }
    }

    // NEW: Calculate recycling payback based on quality
    private Double calculateRecyclingPayback(RecyclableItemDto recyclable) {
        Double baseRate = qualityRefundRates.get(recyclable.getQuality());
        if (baseRate == null) {
            baseRate = 4.0; // Default rate
        }

        // Apply material-specific multipliers if needed
        Double materialMultiplier = getMaterialMultiplier(recyclable.getType());

        return recyclable.getWeightKg() * baseRate * materialMultiplier;
    }

    // NEW: Material-specific multipliers (can be configurable)
    private Double getMaterialMultiplier(RecyclableType type) {
        switch (type) {
            case METAL: return 1.2;      // Metal is more valuable
            case ELECTRONICS: return 1.5; // Electronics are most valuable
            case PAPER: return 0.8;      // Paper is less valuable
            default: return 1.0;         // Plastic, Glass standard value
        }
    }

    // FIXED: Updated recordRecyclingCollection method with correct parameters
    public CollectionEvent recordRecyclingCollection(RecyclingRequestDto request) {
        // Implementation for recycling collections
        User resident = userRepository.findById(request.getResidentId())
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        // Use default quality if not provided
        QualityGrade quality = request.getQuality() != null ? request.getQuality() : QualityGrade.AVERAGE;

        // FIXED: Use the new method signature with RecyclableType and QualityGrade
        Double paybackAmount = calculateRecyclingPaybackWithQuality(
                request.getWeight(),
                request.getRecyclableType(),
                quality
        );

        // Create recycling collection record
        RecyclingCollection recycling = new RecyclingCollection();
        recycling.setCollectionTime(LocalDateTime.now());
        recycling.setRecyclableType(request.getRecyclableType());
        recycling.setWeight(request.getWeight());
        recycling.setPaybackAmount(paybackAmount);
        recycling.setQuality(quality);

        recycling.setResident(resident);

        RecyclingCollection savedRecycling = recyclingCollectionRepository.save(recycling);

        // Update resident recycling credits
        updateResidentRecyclingCredits(resident, paybackAmount);

        return null; // Return appropriate response
    }

    // NEW: Helper method for recycling payback with quality
    private Double calculateRecyclingPaybackWithQuality(Double weight, RecyclableType recyclableType, QualityGrade quality) {
        Double baseRate = qualityRefundRates.get(quality);
        if (baseRate == null) {
            baseRate = 4.0; // Default rate
        }

        // Apply material-specific multipliers
        Double materialMultiplier = getMaterialMultiplier(recyclableType);

        return weight * baseRate * materialMultiplier;
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

    // REMOVED: The problematic method that was causing compilation errors
    /*
    private Double calculateRecyclingPayback(BillingModel model, Double weight, BinType wasteType) {
        // This method is removed as it's no longer compatible with our new system
    }
    */

    private boolean isCollectionScheduledForToday(String binId, LocalDate today) {
        Optional<CollectionSchedule> schedule = collectionScheduleRepository
                .findPendingScheduleForBin(binId, today);
        return schedule.isPresent();
    }

    private void updateCollectionScheduleStatus(String binId, LocalDate date) {
        collectionScheduleRepository.findPendingScheduleForBin(binId, date)
                .ifPresent(schedule -> {
                    schedule.setStatus(ScheduleStatus.COMPLETED);
                    collectionScheduleRepository.save(schedule);
                });
    }

    private void updateBinLevelAfterCollection(WasteBin bin) {
        bin.setCurrentLevel(0.0); // Reset level after collection
        wasteBinRepository.save(bin);
    }

    private void updateResidentPendingCharges(User resident, Double charge) {
        Double currentCharges = resident.getPendingCharges() != null ? resident.getPendingCharges() : 0.0;
        resident.setPendingCharges(currentCharges + charge);
        userRepository.save(resident);

        // AUTO-INVOICE: Generate invoice if charges exceed threshold
        if (resident.getPendingCharges() >= 50.0) {
            try {
                invoiceService.generateMonthlyInvoice(resident.getId());
            } catch (Exception e) {
                System.err.println("Auto-invoice failed: " + e.getMessage());
            }
        }
    }

    private void updateResidentRecyclingCredits(User resident, Double credits) {
        Double currentCredits = resident.getRecyclingCredits() != null ? resident.getRecyclingCredits() : 0.0;
        resident.setRecyclingCredits(currentCredits + credits);
        userRepository.save(resident);
    }

    // Schedule-related methods
    public Optional<CollectionSchedule> getTodayScheduleForBin(String binId) {
        return collectionScheduleRepository.findPendingScheduleForBin(binId, LocalDate.now());
    }

    public boolean isBinScheduledForCollectionToday(String binId) {
        return getTodayScheduleForBin(binId).isPresent();
    }

    public CollectionSchedule getScheduleDetails(String binId, LocalDate date) {
        return collectionScheduleRepository.findPendingScheduleForBin(binId, date)
                .orElseThrow(() -> new RuntimeException("No schedule found for bin " + binId + " on " + date));
    }

    // Existing methods from your current implementation
    public List<CollectionEvent> getCollectionsByBin(String binId) {
        return collectionRepository.findByWasteBinBinId(binId);
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

    public List<RecyclingCollection> getUninvoicedRecycling() {
        return recyclingCollectionRepository.findByInvoiceIsNull();
    }

    // Repository getter methods for other services
    public CollectionEventRepository getCollectionEventRepository() {
        return collectionRepository;
    }

    public RecyclingCollectionRepository getRecyclingCollectionRepository() {
        return recyclingCollectionRepository;
    }

    // Bin level update method for residents
    public WasteBin updateBinLevel(String binId, Double newLevel) {
        if (newLevel < 0 || newLevel > 100) {
            throw new RuntimeException("Bin level must be between 0 and 100");
        }

        WasteBin bin = wasteBinRepository.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binId));

        bin.setCurrentLevel(newLevel);

        // Auto-schedule collection if bin is nearly full
        if (newLevel >= 80) {
            autoScheduleCollection(bin);
        }

        return wasteBinRepository.save(bin);
    }

    private void autoScheduleCollection(WasteBin bin) {
        // Check if there's already a pending schedule for the next 2 days
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = LocalDate.now().plusDays(2);

        boolean hasSchedule = collectionScheduleRepository.findPendingScheduleForBin(bin.getBinId(), tomorrow).isPresent() ||
                collectionScheduleRepository.findPendingScheduleForBin(bin.getBinId(), dayAfter).isPresent();

        if (!hasSchedule) {
            // Auto-schedule for tomorrow
            CollectionSchedule schedule = new CollectionSchedule();
            schedule.setWasteBin(bin);
            schedule.setScheduledDate(tomorrow);
            schedule.setStatus(ScheduleStatus.PENDING);
            schedule.setNotes("Auto-scheduled: Bin reached " + bin.getCurrentLevel() + "% capacity");

            collectionScheduleRepository.save(schedule);
        }
    }

    // Method to get collection summary for staff app
    public Object getCollectionSummary(String binId) {
        WasteBin bin = wasteBinRepository.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found"));

        Optional<CollectionSchedule> todaySchedule = getTodayScheduleForBin(binId);

        return new Object() {
            public final String binId = bin.getBinId();
            public final String location = bin.getLocation();
            public final BinType binType = bin.getBinType();
            public final Double currentLevel = bin.getCurrentLevel();
            public final boolean scheduledToday = todaySchedule.isPresent();
            public final String residentName = bin.getResident() != null ? bin.getResident().getName() : "Unassigned";
        };
    }
}