package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.ScheduleRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.ScheduleResponseDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionSchedule;
import com.CSSEProject.SmartWasteManagement.waste.entity.ScheduleStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionScheduleRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private CollectionScheduleRepository scheduleRepository;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    public CollectionSchedule createCollectionSchedule(ScheduleRequestDto request) {
        WasteBin bin = wasteBinRepository.findById(request.getBinId())
                .orElseThrow(() -> new RuntimeException("Bin not found: " + request.getBinId()));

        // REMOVED: Check for existing schedule - allow multiple schedules
        // This allows creating new schedules even if one exists

        CollectionSchedule schedule = new CollectionSchedule();
        schedule.setWasteBin(bin);
        schedule.setScheduledDate(request.getScheduledDate());
        // REMOVED: scheduledTime
        schedule.setNotes(request.getNotes());
        schedule.setStatus(ScheduleStatus.PENDING);

        return scheduleRepository.save(schedule);
    }

    public List<ScheduleResponseDto> getSchedulesByBin(String binId) {
        List<CollectionSchedule> schedules = scheduleRepository.findByWasteBinBinId(binId);
        return schedules.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponseDto> getSchedulesByResident(Long residentId) {
        List<CollectionSchedule> schedules = scheduleRepository.findByWasteBinResidentId(residentId);
        return schedules.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ScheduleResponseDto> getTodayScheduleForBin(String binId) {
        Optional<CollectionSchedule> schedule = scheduleRepository.findPendingScheduleForBin(binId, LocalDate.now());
        return schedule.map(this::convertToDto);
    }

    public CollectionSchedule cancelSchedule(Long scheduleId) {
        CollectionSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setStatus(ScheduleStatus.CANCELLED);
        return scheduleRepository.save(schedule);
    }

    public List<ScheduleResponseDto> getPendingSchedulesForToday() {
        List<CollectionSchedule> schedules = scheduleRepository.findByScheduledDateAndStatus(LocalDate.now(), ScheduleStatus.PENDING);
        return schedules.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Helper method to convert Entity to DTO
    private ScheduleResponseDto convertToDto(CollectionSchedule schedule) {
        ScheduleResponseDto dto = new ScheduleResponseDto();
        dto.setId(schedule.getId());
        dto.setBinId(schedule.getBinId());
        dto.setScheduledDate(schedule.getScheduledDate());
        // REMOVED: scheduledTime
        dto.setStatus(schedule.getStatus());
        dto.setNotes(schedule.getNotes());
        dto.setCreatedAt(schedule.getCreatedAt());
        dto.setResidentId(schedule.getResidentId());

        // Add bin details
        if (schedule.getWasteBin() != null) {
            WasteBin bin = schedule.getWasteBin();
            dto.setBinType(bin.getBinType() != null ? bin.getBinType().name() : null);
            dto.setLocation(bin.getLocation());
            dto.setCurrentLevel(bin.getCurrentLevel());
        }

        return dto;
    }
}