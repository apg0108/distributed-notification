package com.sagant.distributednotification.service;

import java.util.List;

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
      final List<Notification> retryable = notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, maxAttempts);

      for (final Notification notification : retryable) {
         notification.setRetryCount(notification.getRetryCount() + 1);
         log.info("Retrying notification {} (attempt {}/{})", notification.getId(), notification.getRetryCount(), maxAttempts);
         notificationProcessingListener.attemptDispatch(notification);
      }
   }
}
