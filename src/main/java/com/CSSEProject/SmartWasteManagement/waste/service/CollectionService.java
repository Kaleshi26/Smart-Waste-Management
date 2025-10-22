package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.CollectionResponseDto;
import com.CSSEProject.SmartWasteManagement.dto.RecyclingRequestDto;
import com.CSSEProject.SmartWasteManagement.payment.controller.InvoiceController;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.entity.Invoice;
import com.CSSEProject.SmartWasteManagement.payment.entity.InvoiceStatus;
import com.CSSEProject.SmartWasteManagement.payment.repository.InvoiceRepository;
import com.CSSEProject.SmartWasteManagement.payment.service.BillingService;
import com.CSSEProject.SmartWasteManagement.payment.service.InvoiceService;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionSchedule;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclingCollection;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.ScheduleStatus;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionScheduleRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.RecyclingCollectionRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    // NEW: Get collections as DTOs directly
    public List<CollectionResponseDto> getCollectionsByCollectorAsDto(Long collectorId) {
        List<CollectionEvent> collections = getCollectionsByCollector(collectorId);
        return collections.stream()
                .map(CollectionResponseDto::new)
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

    private Invoice generateInvoiceAfterCollection(User resident, Double charge, Double weight, BinType binType) {
        try {
            System.out.println("🧾 AUTO-GENERATING INVOICE AFTER COLLECTION");
            System.out.println("   - Resident: " + resident.getName());
            System.out.println("   - Charge: $" + charge);
            System.out.println("   - Weight: " + weight + " kg");
            System.out.println("   - Bin Type: " + binType);

            // Create invoice immediately
            Invoice invoice = new Invoice();
            invoice.setResident(resident);
            invoice.setInvoiceNumber("INV-AUTO-" + System.currentTimeMillis());
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setDueDate(LocalDate.now().plusDays(30));
            invoice.setPeriodStart(LocalDate.now());
            invoice.setPeriodEnd(LocalDate.now());
            invoice.setBaseCharge(0.0);
            invoice.setWeightBasedCharge(charge);
            invoice.setRecyclingCredits(0.0);
            invoice.setTotalAmount(charge);
            invoice.setStatus(InvoiceStatus.PENDING);

            Invoice savedInvoice = invoiceRepository.save(invoice);

            System.out.println("✅ AUTO-INVOICE GENERATED: " + savedInvoice.getInvoiceNumber());
            System.out.println("   - Amount: $" + savedInvoice.getTotalAmount());
            System.out.println("   - Due Date: " + savedInvoice.getDueDate());

            return savedInvoice;

        } catch (Exception e) {
            System.err.println("❌ AUTO-INVOICE FAILED: " + e.getMessage());
            return null;
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

        CollectionEvent savedCollection = collectionRepository.save(collection);

        // 6. Update collection schedule status
        updateCollectionScheduleStatus(bin.getBinId(), collectionTime.toLocalDate());

        // 7. ✅ AUTO-GENERATE INVOICE IMMEDIATELY AFTER COLLECTION
        Invoice autoInvoice = generateInvoiceAfterCollection(resident, charge, request.getWeight(), bin.getBinType());

        // Link this collection to the auto-generated invoice
        savedCollection.setInvoice(autoInvoice);
        collectionRepository.save(savedCollection);

        // 8. Update bin level (reset to 0 after collection)
        updateBinLevelAfterCollection(bin);

        // 9. Provide feedback
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

        // Create recycling collection record
        RecyclingCollection recycling = new RecyclingCollection();
        recycling.setCollectionTime(LocalDateTime.now());
        recycling.setWasteType(request.getWasteType());
        recycling.setWeight(request.getWeight());
        recycling.setPaybackAmount(paybackAmount);
        recycling.setResident(resident);

        RecyclingCollection savedRecycling = recyclingCollectionRepository.save(recycling);

        // Update resident recycling credits
        updateResidentRecyclingCredits(resident, paybackAmount);

        return null; // Return appropriate response
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