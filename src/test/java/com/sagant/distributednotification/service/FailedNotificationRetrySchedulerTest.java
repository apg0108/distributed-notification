package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.sagant.distributednotification.config.property.NotificationRetryProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class FailedNotificationRetrySchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationProcessingListener notificationProcessingListener;

    private FailedNotificationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FailedNotificationRetryScheduler(notificationRepository, notificationProcessingListener, new NotificationRetryProperties(1));
    }

    @Test
    void retryFailedNotifications_incrementsRetryCountAndDelegatesDispatch() {
        final Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(0);

        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 1)).thenReturn(List.of(notification));

        scheduler.retryFailedNotifications();

        assertThat(notification.getRetryCount()).isEqualTo(1);
        verify(notificationProcessingListener).attemptDispatch(notification);
        assertThat(MDC.get(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void retryFailedNotifications_dispatchesHighestPriorityFirst() {
        final Notification low = failedNotification(NotificationPriority.LOW, Instant.now().minusSeconds(300));
        final Notification high = failedNotification(NotificationPriority.HIGH, Instant.now().minusSeconds(10));
        final Notification medium = failedNotification(NotificationPriority.MEDIUM, Instant.now().minusSeconds(200));

        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 1)).thenReturn(List.of(low, high, medium));

        scheduler.retryFailedNotifications();

        final InOrder inOrder = inOrder(notificationProcessingListener);
        inOrder.verify(notificationProcessingListener).attemptDispatch(high);
        inOrder.verify(notificationProcessingListener).attemptDispatch(medium);
        inOrder.verify(notificationProcessingListener).attemptDispatch(low);
    }

    @Test
    void retryFailedNotifications_withSamePriority_dispatchesOldestFirst() {
        final Instant now = Instant.now();
        final Notification newer = failedNotification(NotificationPriority.HIGH, now.minusSeconds(10));
        final Notification older = failedNotification(NotificationPriority.HIGH, now.minusSeconds(600));

        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 1)).thenReturn(List.of(newer, older));

        scheduler.retryFailedNotifications();

        final InOrder inOrder = inOrder(notificationProcessingListener);
        inOrder.verify(notificationProcessingListener).attemptDispatch(older);
        inOrder.verify(notificationProcessingListener).attemptDispatch(newer);
    }

    @Test
    void retryFailedNotifications_whenNoneEligible_doesNothing() {
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 1)).thenReturn(List.of());

        scheduler.retryFailedNotifications();

        verifyNoInteractions(notificationProcessingListener);
    }

    private Notification failedNotification(final NotificationPriority priority, final Instant createdAt) {
        final Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(0);
        notification.setPriority(priority);
        notification.setCreatedAt(createdAt);
        return notification;
    }
}
