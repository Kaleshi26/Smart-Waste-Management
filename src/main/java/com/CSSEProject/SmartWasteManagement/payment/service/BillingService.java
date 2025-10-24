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

    // ADD THIS METHOD for better debugging
// ENHANCED: Get billing model with better debugging
    public BillingModel getActiveBillingModelForResident(Long residentId) {
        try {
            User resident = userService.getUserById(residentId);
            String city = extractCityFromAddress(resident.getAddress());

            System.out.println("🔍 Billing Model Lookup:");
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

    private String extractCityFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            System.out.println("⚠️  Address is null or empty, using 'Default'");
            return "Default";
        }

        String lowerAddress = address.toLowerCase().trim();
        System.out.println("🔍 Extracting city from address: " + address);

        // Common Sri Lankan cities with variations
        if (lowerAddress.contains("colombo")) return "Colombo";
        if (lowerAddress.contains("kandy")) return "Kandy";
        if (lowerAddress.contains("gampaha")) return "Gampaha";
        if (lowerAddress.contains("galle")) return "Galle";
        if (lowerAddress.contains("jaffna")) return "Jaffna";
        if (lowerAddress.contains("negombo")) return "Negombo";
        if (lowerAddress.contains("kurunegala")) return "Kurunegala";
        if (lowerAddress.contains("anuradhapura")) return "Anuradhapura";
        if (lowerAddress.contains("ratnapura")) return "Ratnapura";
        if (lowerAddress.contains("badulla")) return "Badulla";
        if (lowerAddress.contains("matara")) return "Matara";
        if (lowerAddress.contains("kegalle")) return "Kegalle";

        // Try to extract city from common address formats
        String[] parts = address.split(",");
        if (parts.length > 1) {
            // Usually format: "Street, City, Province"
            String possibleCity = parts[parts.length - 2].trim(); // Second last part is often city
            System.out.println("📍 Extracted possible city: " + possibleCity);
            return possibleCity;
        } else if (parts.length == 1) {
            // Single part address, use the whole thing
            return address.trim();
        }

        System.out.println("⚠️  Could not extract city, using 'Default'");
        return "Default";
    }
}