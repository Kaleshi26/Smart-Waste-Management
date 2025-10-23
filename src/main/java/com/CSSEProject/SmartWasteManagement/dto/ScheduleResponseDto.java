package com.CSSEProject.SmartWasteManagement.dto;

import com.CSSEProject.SmartWasteManagement.waste.entity.ScheduleStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleResponseDto {
    private Long id;
    private String binId;
    private LocalDate scheduledDate;
    // REMOVED: scheduledTime
    private ScheduleStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private Long residentId;

    // Add bin details for frontend display
    private String binType;
    private String location;
    private Double currentLevel;

    public ScheduleResponseDto() {}
}