// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/dto/RecyclingRequestDto.java
package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.QualityGrade;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclableType;
import lombok.Data;

@Data
public class RecyclingRequestDto {
    private Long residentId;
    private RecyclableType recyclableType;
    private Double weight;
    private QualityGrade quality; // ADD THIS FIELD
    private String notes; // ADD THIS FIELD

    // Optional: Constructor for convenience
    public RecyclingRequestDto() {}

    public RecyclingRequestDto(Long residentId, RecyclableType recyclableType, Double weight, QualityGrade quality, String notes) {
        this.residentId = residentId;
        this.recyclableType = recyclableType;
        this.weight = weight;
        this.quality = quality;
        this.notes = notes;
    }
}