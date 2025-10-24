package com.CSSEProject.SmartWasteManagement.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleRequestDto {
    private String binId;
    private LocalDate scheduledDate;
    // REMOVED: scheduledTime
    private String notes;
}