// File: src/test/java/com/CSSEProject/SmartWasteManagement/reporting/service/ReportingServiceTest.java
package com.CSSEProject.SmartWasteManagement.reporting.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionEventDto;
import com.CSSEProject.SmartWasteManagement.dto.DashboardStatsDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private CollectionEventRepository collectionEventRepository;

    @Mock
    private WasteBinRepository wasteBinRepository;

    @InjectMocks
    private ReportingService reportingService;

    private User resident;
    private User staff;
    private WasteBin bin;
    private CollectionEvent event1;
    private CollectionEvent event2;

    @BeforeEach
    void setUp() {
        // Create mock data for our tests
        resident = new User();
        resident.setId(1L);
        resident.setName("Test Resident");

        staff = new User();
        staff.setId(2L);
        staff.setName("Test Staff");

        bin = new WasteBin();
        bin.setId(101L);
        bin.setBinId("BIN-TEST-01");
        bin.setResident(resident);

        event1 = new CollectionEvent();
        event1.setId(1001L);
        event1.setWasteBin(bin);
        event1.setStaffMember(staff);
        event1.setWeightInKg(10.5);
        event1.setCollectionTime(LocalDateTime.now());

        event2 = new CollectionEvent();
        event2.setId(1002L);
        event2.setWasteBin(bin);
        event2.setStaffMember(staff);
        event2.setWeightInKg(5.5);
        event2.setCollectionTime(LocalDateTime.now().minusDays(1));
    }

    @Test
    void getDashboardStats_shouldReturnCorrectCalculations() {
        // --- ARRANGE ---
        // Tell our fake repositories what to return when called
        when(collectionEventRepository.findAll()).thenReturn(List.of(event1, event2));
        when(wasteBinRepository.count()).thenReturn(5L); // Let's pretend there are 5 bins

        // --- ACT ---
        DashboardStatsDto stats = reportingService.getDashboardStats(null, null);

        // --- ASSERT ---
        // Check if the calculations are correct
        assertNotNull(stats);
        assertEquals(2, stats.getTotalCollections()); // event1 + event2 = 2 collections
        assertEquals(16.0, stats.getTotalWeightKg()); // 10.5 + 5.5 = 16.0
        assertEquals(5, stats.getTotalBins());
    }

    @Test
    void getCollectionEvents_shouldReturnCorrectlyMappedDtos() {
        // --- ARRANGE ---
        when(collectionEventRepository.findAll()).thenReturn(List.of(event1, event2));

        // --- ACT ---
        List<CollectionEventDto> dtos = reportingService.getCollectionEvents(null, null);

        // --- ASSERT ---
        assertNotNull(dtos);
        assertEquals(2, dtos.size()); // We should get two DTOs back

        // Check if the data was mapped correctly for the first event
        CollectionEventDto firstDto = dtos.get(0);
        assertEquals(event1.getId(), firstDto.getId());
        assertEquals("BIN-TEST-01", firstDto.getBinId());
        assertEquals("Test Resident", firstDto.getResidentName());
        assertEquals("Test Staff", firstDto.getStaffName());
        assertEquals(10.5, firstDto.getWeightInKg());
    }
}