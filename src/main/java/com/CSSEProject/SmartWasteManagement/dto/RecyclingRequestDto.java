package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import lombok.Data;

@Data
public class RecyclingRequestDto {
    private String binId;
    private Long staffId;
    private BinType wasteType;
    private Double weight;
    private Long residentId;
}