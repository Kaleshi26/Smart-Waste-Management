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

    public BillingModel getActiveBillingModelForResident(Long residentId) {
        User resident = userService.getUserById(residentId);
        // In a real system, you'd determine city from resident's address
        String city = extractCityFromAddress(resident.getAddress());
        
        return billingModelRepository.findByCityAndActiveTrue(city)
                .orElseThrow(() -> new RuntimeException("No active billing model found for city: " + city));
    }

    public BillingModel getBillingModelById(Long id) {
        return billingModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Billing model not found with id: " + id));
    }

    public List<BillingModel> getBillingModelsByCity(String city) {
        return billingModelRepository.findByCity(city);
    }

    public List<BillingModel> getActiveBillingModels() {
        return billingModelRepository.findByActiveTrue();
    }

    public BillingModel deactivateBillingModel(Long modelId) {
        BillingModel model = getBillingModelById(modelId);
        model.setActive(false);
        return billingModelRepository.save(model);
    }

    private String extractCityFromAddress(String address) {
        // Simple extraction - in real system, use proper address parsing
        if (address.toLowerCase().contains("colombo")) return "Colombo";
        if (address.toLowerCase().contains("kandy")) return "Kandy";
        if (address.toLowerCase().contains("gampaha")) return "Gampaha";
        return "Default";
    }
}