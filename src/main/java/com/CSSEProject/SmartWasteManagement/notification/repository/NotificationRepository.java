package com.CSSEProject.SmartWasteManagement.notification.repository;

import com.CSSEProject.SmartWasteManagement.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
