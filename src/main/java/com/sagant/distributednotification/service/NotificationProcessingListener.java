package com.sagant.distributednotification.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;
import com.sagant.distributednotification.service.sender.NotificationSenderResolver;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingListener {

   static final String NOTIFICATION_ID_MDC_KEY = "notificationId";

   private final NotificationRepository notificationRepository;

   private final NotificationSenderResolver notificationSenderResolver;

   private final MeterRegistry meterRegistry;

   @Async("notificationTaskExecutor")
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void onNotificationCreated(final UUID notificationId) {
      MDC.put(NOTIFICATION_ID_MDC_KEY, notificationId.toString());
      try {
         final Optional<Notification> optNotification = notificationRepository.findById(notificationId);
         if (optNotification.isEmpty() || optNotification.get().getStatus() != NotificationStatus.PENDING) {
            log.warn("Skipping processing for notification {}: no longer PENDING", notificationId);
            return;
         }
         attemptDispatch(optNotification.get());
      } finally {
         MDC.remove(NOTIFICATION_ID_MDC_KEY);
      }
   }

   void attemptDispatch(final Notification notification) {
      try {
         dispatch(notification);
         notification.setStatus(NotificationStatus.SENT);
         notification.setSentAt(Instant.now());
         meterRegistry.counter("notifications.sent", "channel", notification.getChannel().name()).increment();
      } catch (final Exception ex) {
         notification.setLastError(ex.getMessage());
         notification.setStatus(NotificationStatus.FAILED);
         log.error("Notification {} failed: {}", notification.getId(), ex.getMessage());
         meterRegistry.counter("notifications.failed", "channel", notification.getChannel().name()).increment();
      }
      notificationRepository.save(notification);
   }

   void dispatch(final Notification notification) {
      notificationSenderResolver.resolve(notification.getChannel()).send(notification);
   }
}
