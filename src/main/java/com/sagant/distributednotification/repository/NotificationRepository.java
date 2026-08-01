package com.sagant.distributednotification.repository;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
