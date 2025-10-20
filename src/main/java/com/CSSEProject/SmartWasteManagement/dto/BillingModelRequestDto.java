package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.payment.entity.BillingType;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import lombok.Data;
import java.util.Map;

@Data
public class BillingModelRequestDto {
    private String city;
    private BillingType billingType;
    private Double ratePerKg;
    private Double monthlyFlatFee;
    private Double baseFee;
    private Double additionalRatePerKg;
    private Map<BinType, Double> recyclingPaybackRates;
}