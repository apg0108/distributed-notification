package com.sagant.distributednotification.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

   List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int retryCount);

   List<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, Instant cutoff);
}
