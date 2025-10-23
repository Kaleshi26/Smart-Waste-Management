// File: backend/src/main/java/com/CSSEProject/SmartWasteManagement/dto/RecyclableItemDto.java
package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.QualityGrade;
import com.CSSEProject.SmartWasteManagement.waste.entity.RecyclableType;
import lombok.Data;

@Data
public class RecyclableItemDto {
    private RecyclableType type;      // PLASTIC, PAPER, METAL, GLASS, ELECTRONICS
    private Double weightKg;
    private QualityGrade quality;     // GOOD, AVERAGE, POOR (affects refund rate)
    private String notes;
}