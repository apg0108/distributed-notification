package com.sagant.distributednotification.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sagant.distributednotification.config.property.NotificationRecoveryProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StuckNotificationRecoveryScheduler {

   private static final List<NotificationStatus> RECOVERABLE_STATUSES = List.of(NotificationStatus.PENDING, NotificationStatus.PROCESSING);

   private final NotificationRepository notificationRepository;

   private final NotificationProcessingListener notificationProcessingListener;

   private final NotificationRecoveryProperties notificationRecoveryProperties;

   @Scheduled(fixedRateString = "${notification.recovery.fixed-rate-ms:60000}")
   public void recoverStuckNotifications() {
      final Instant cutoff = Instant.now().minusMillis(notificationRecoveryProperties.timeoutMs());
      final List<Notification> stuck = notificationRepository.findByStatusInAndCreatedAtBefore(RECOVERABLE_STATUSES, cutoff);

      for (final Notification notification : stuck) {
         MDC.put(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY, notification.getId().toString());
         try {
            log.warn("Recovering notification {} stuck in {} since {}", notification.getId(), notification.getStatus(), notification.getUpdatedAt());
            notificationProcessingListener.attemptDispatch(notification);
         } finally {
            MDC.remove(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY);
         }
      }
   }
}
