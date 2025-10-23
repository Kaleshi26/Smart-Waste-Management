package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.OfflineCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OfflineCollectionRepository extends JpaRepository<OfflineCollection, Long> {
    List<OfflineCollection> findByDeviceIdAndSyncedFalse(String deviceId);
    List<OfflineCollection> findBySyncedFalse();
    Long countByDeviceIdAndSyncedFalse(String deviceId);
    List<OfflineCollection> findByDeviceId(String deviceId);
}