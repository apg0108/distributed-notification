package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.sagant.distributednotification.config.property.NotificationRecoveryProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class StuckNotificationRecoverySchedulerTest {

   @Mock
   private NotificationRepository notificationRepository;

   @Mock
   private NotificationProcessingListener notificationProcessingListener;

   private StuckNotificationRecoveryScheduler scheduler;

   @BeforeEach
   void setUp() {
      scheduler = new StuckNotificationRecoveryScheduler(notificationRepository, notificationProcessingListener,
            new NotificationRecoveryProperties(120_000));
   }

   @Test
   void recoverStuckNotifications_reDispatchesStuckNotifications() {
      final Notification notification = new Notification();
      notification.setId(UUID.randomUUID());
      notification.setStatus(NotificationStatus.PENDING);
      notification.setUpdatedAt(Instant.now().minusSeconds(300));

      when(notificationRepository.findByStatusInAndCreatedAtBefore(anyCollection(), any(Instant.class))).thenReturn(List.of(notification));

      scheduler.recoverStuckNotifications();

      verify(notificationProcessingListener).attemptDispatch(notification);
      assertThat(MDC.get(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY)).isNull();
   }

   @Test
   void recoverStuckNotifications_passesPendingAndProcessingStatusesWithCutoffInThePast() {
      when(notificationRepository.findByStatusInAndCreatedAtBefore(anyCollection(), any(Instant.class))).thenReturn(List.of());

      final Instant beforeRun = Instant.now();
      scheduler.recoverStuckNotifications();

      final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
      verify(notificationRepository).findByStatusInAndCreatedAtBefore(eq(List.of(NotificationStatus.PENDING, NotificationStatus.PROCESSING)),
            cutoffCaptor.capture());

      assertThat(cutoffCaptor.getValue()).isBefore(beforeRun);
   }

   @Test
   void recoverStuckNotifications_whenNoneStuck_doesNothing() {
      when(notificationRepository.findByStatusInAndCreatedAtBefore(anyCollection(), any(Instant.class))).thenReturn(List.of());

      scheduler.recoverStuckNotifications();

      verifyNoInteractions(notificationProcessingListener);
   }
}
