package com.CSSEProject.SmartWasteManagement.payment.service;

import com.CSSEProject.SmartWasteManagement.dto.BillingModelRequestDto;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingModel;
import com.CSSEProject.SmartWasteManagement.payment.entity.BillingType;
import com.CSSEProject.SmartWasteManagement.payment.repository.BillingModelRepository;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    @Autowired
    private BillingModelRepository billingModelRepository;

    @Autowired
    private UserService userService;

    public BillingModel createBillingModel(BillingModelRequestDto requestDto) {
        // Check if active model already exists for this city
        if (billingModelRepository.existsByCityAndActiveTrue(requestDto.getCity())) {
            throw new RuntimeException("Active billing model already exists for city: " + requestDto.getCity());
        }

        BillingModel model = new BillingModel();
        model.setCity(requestDto.getCity());
        model.setBillingType(requestDto.getBillingType());
        model.setRatePerKg(requestDto.getRatePerKg());
        model.setMonthlyFlatFee(requestDto.getMonthlyFlatFee());
        model.setBaseFee(requestDto.getBaseFee());
        model.setAdditionalRatePerKg(requestDto.getAdditionalRatePerKg());
        model.setRecyclingPaybackRates(requestDto.getRecyclingPaybackRates());
        model.setActive(true);

        return billingModelRepository.save(model);
    }

    public BillingModel updateBillingModel(Long modelId, BillingModelRequestDto requestDto) {
        BillingModel model = getBillingModelById(modelId);

        model.setBillingType(requestDto.getBillingType());
        model.setRatePerKg(requestDto.getRatePerKg());
        model.setMonthlyFlatFee(requestDto.getMonthlyFlatFee());
        model.setBaseFee(requestDto.getBaseFee());
        model.setAdditionalRatePerKg(requestDto.getAdditionalRatePerKg());
        model.setRecyclingPaybackRates(requestDto.getRecyclingPaybackRates());

        return billingModelRepository.save(model);
    }

    // UPDATED: Now uses Optional from repository
    public BillingModel getActiveBillingModelForCity(String city) {
        System.out.println("🔍 Looking for active billing model for city: " + city);

        Optional<BillingModel> modelOpt = billingModelRepository.findByCityAndActiveTrue(city);

        if (modelOpt.isPresent()) {
            BillingModel model = modelOpt.get();
            System.out.println("✅ Found billing model for " + city + ": " + model.getBillingType() + " - Rate: $" + model.getRatePerKg() + "/kg");
            return model;
        } else {
            System.out.println("❌ No active billing model found for city: " + city);

            // Try to find any active model as fallback
            List<BillingModel> anyActive = billingModelRepository.findByActiveTrue();
            if (!anyActive.isEmpty()) {
                BillingModel fallbackModel = anyActive.get(0);
                System.out.println("🔄 Using fallback billing model: " + fallbackModel.getCity());
                return fallbackModel;
            }

            System.out.println("💥 No active billing models found in system!");
            return null;
        }
    }

    // UPDATED: Uses Optional properly
    public BillingModel getActiveBillingModelForResident(Long residentId) {
        try {
            User resident = userService.getUserById(residentId);
            String city = extractCityFromAddress(resident.getAddress());

            System.out.println("🔍 Billing Model Lookup for Resident:");
            System.out.println("   - Resident: " + resident.getName());
            System.out.println("   - Address: " + resident.getAddress());
            System.out.println("   - Extracted City: " + city);

            Optional<BillingModel> modelOpt = billingModelRepository.findByCityAndActiveTrue(city);

            if (modelOpt.isPresent()) {
                BillingModel model = modelOpt.get();
                System.out.println("✅ Found Billing Model:");
                System.out.println("   - City: " + model.getCity());
                System.out.println("   - Type: " + model.getBillingType());
                System.out.println("   - Rate: $" + model.getRatePerKg() + "/kg");
                System.out.println("   - Flat Fee: $" + model.getMonthlyFlatFee());
                System.out.println("   - Base Fee: $" + model.getBaseFee());
                System.out.println("   - Additional Rate: $" + model.getAdditionalRatePerKg() + "/kg");
                return model;
            } else {
                // Try to find any active model as fallback
                List<BillingModel> activeModels = billingModelRepository.findByActiveTrue();
                if (!activeModels.isEmpty()) {
                    BillingModel fallbackModel = activeModels.get(0);
                    System.out.println("⚠️  No billing model for city '" + city + "', using fallback: " + fallbackModel.getCity());
                    return fallbackModel;
                } else {
                    throw new RuntimeException("No active billing models found in system");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting billing model for resident " + residentId + ": " + e.getMessage());
            throw new RuntimeException("Billing configuration error: " + e.getMessage());
        }
    }

    public BillingModel getBillingModelById(Long id) {
        return billingModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Billing model not found with id: " + id));
    }

    public List<BillingModel> getBillingModelsByCity(String city) {
        return billingModelRepository.findByCity(city);
    }

    public BillingModel deactivateBillingModel(Long modelId) {
        BillingModel model = getBillingModelById(modelId);
        model.setActive(false);
        return billingModelRepository.save(model);
    }

    public List<BillingModel> getActiveBillingModels() {
        return billingModelRepository.findByActiveTrue();
    }

    // ENHANCED: Better city extraction with more Sri Lankan cities
    private String extractCityFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            System.out.println("⚠️  Address is null or empty, using 'Colombo' as default");
            return "Colombo";
        }

        String lowerAddress = address.toLowerCase().trim();
        System.out.println("🔍 Extracting city from address: " + address);

        // Comprehensive Sri Lankan cities list
        String[] cities = {
                "colombo", "kandy", "gampaha", "galle", "jaffna", "negombo",
                "kurunegala", "anuradhapura", "ratnapura", "badulla", "matara",
                "kegalle", "kalutara", "matale", "puttalam", "batticaloa", "trincomalee",
                "hambantota", "vavuniya", "kilinochchi", "mannar", "nuwara eliya",
                "polonnaruwa", "moneragala", "ampara", "mullaitivu"
        };

        for (String city : cities) {
            if (lowerAddress.contains(city)) {
                System.out.println("📍 Matched city: " + city);
                return capitalizeWords(city);
            }
        }

        // Try to extract city from common address formats
        String[] parts = address.split(",");
        if (parts.length > 1) {
            // Usually format: "Street, City, Province"
            String possibleCity = parts[parts.length - 2].trim();
            System.out.println("📍 Extracted possible city from address format: " + possibleCity);
            return possibleCity;
        } else if (parts.length == 1) {
            // Single part address, use the whole thing
            System.out.println("📍 Using entire address as city: " + address.trim());
            return address.trim();
        }

        System.out.println("⚠️  Could not extract city, using 'Colombo' as default");
        return "Colombo";
    }

    // Helper method to capitalize city names
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // DEBUG METHOD: Check all billing models
    public void debugAllBillingModels() {
        System.out.println("=== BILLING MODELS DEBUG ===");
        List<BillingModel> allModels = billingModelRepository.findAll();

        if (allModels.isEmpty()) {
            System.out.println("❌ No billing models found in database!");
        } else {
            System.out.println("📋 Found " + allModels.size() + " billing models:");
            for (BillingModel model : allModels) {
                System.out.println("   - ID: " + model.getId() +
                        ", City: " + model.getCity() +
                        ", Type: " + model.getBillingType() +
                        ", Rate: $" + model.getRatePerKg() + "/kg");
            }
        }

        List<BillingModel> activeModels = billingModelRepository.findByActiveTrue();
        System.out.println("✅ Active models: " + activeModels.size());

        System.out.println("=== END DEBUG ===");
    }

    // TEST METHOD: Test city extraction for a specific resident
    public void testResidentBilling(Long residentId) {
        try {
            User resident = userService.getUserById(residentId);
            String city = extractCityFromAddress(resident.getAddress());
            BillingModel model = getActiveBillingModelForResident(residentId);

            System.out.println("🧪 TEST RESULT for Resident " + resident.getName() + ":");
            System.out.println("   - Address: " + resident.getAddress());
            System.out.println("   - Extracted City: " + city);
            System.out.println("   - Billing Model: " + (model != null ?
                    model.getCity() + " (" + model.getBillingType() + ")" : "NULL"));

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
        }
    }
}