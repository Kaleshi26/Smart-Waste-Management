package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionSchedule;
import com.CSSEProject.SmartWasteManagement.waste.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionScheduleRepository extends JpaRepository<CollectionSchedule, Long> {

    List<CollectionSchedule> findByWasteBinBinId(String binId);
    List<CollectionSchedule> findByWasteBinBinIdAndScheduledDate(String binId, LocalDate date);
    List<CollectionSchedule> findByScheduledDateAndStatus(LocalDate date, ScheduleStatus status);

    @Query("SELECT cs FROM CollectionSchedule cs WHERE cs.wasteBin.binId = :binId AND cs.scheduledDate = :date AND cs.status = 'PENDING'")
    Optional<CollectionSchedule> findPendingScheduleForBin(@Param("binId") String binId, @Param("date") LocalDate date);

    // FIXED: Use the relationship path
    @Query("SELECT cs FROM CollectionSchedule cs WHERE cs.wasteBin.resident.id = :residentId")
    List<CollectionSchedule> findByWasteBinResidentId(@Param("residentId") Long residentId);

    List<CollectionSchedule> findByStatus(ScheduleStatus status);
}