package com.sagant.distributednotification.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sagant.distributednotification.config.property.NotificationRetryProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedNotificationRetryScheduler {

   private final NotificationRepository notificationRepository;

   private final NotificationProcessingListener notificationProcessingListener;

   private final NotificationRetryProperties notificationRetryProperties;

   @Scheduled(fixedRateString = "${notification.retry.fixed-rate-ms:30000}")
   public void retryFailedNotifications() {
      final int maxAttempts = notificationRetryProperties.maxAttempts();
      final List<Notification> retryable = new ArrayList<>(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, maxAttempts));
      retryable.sort(Notification.HIGHEST_PRIORITY_FIRST);

      for (final Notification notification : retryable) {
         MDC.put(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY, notification.getId().toString());
         try {
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.info("Retrying notification {} (attempt {}/{})", notification.getId(), notification.getRetryCount(), maxAttempts);
            notificationProcessingListener.attemptDispatch(notification);
         } finally {
            MDC.remove(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY);
         }
      }
   }
}
