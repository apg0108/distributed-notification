package com.sagant.distributednotification.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationCreatedEvent;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingListener {

   private final NotificationRepository notificationRepository;

   @Async("notificationTaskExecutor")
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void onNotificationCreated(final NotificationCreatedEvent event) {
      final Optional<Notification> optNotification = notificationRepository.findById(event.notificationId());
      if (optNotification.isEmpty() || optNotification.get().getStatus() != NotificationStatus.PENDING) {
         log.warn("Skipping processing for notification {}: no longer PENDING", event.notificationId());
         return;
      }

      final Notification notification = optNotification.get();
      log.info("Processing notification {} for recipient {} via channel {}", event.notificationId(), event.recipient(), event.channel());

      notification.setStatus(NotificationStatus.SENT);
      notification.setSentAt(Instant.now());
      notificationRepository.save(notification);
   }
}
