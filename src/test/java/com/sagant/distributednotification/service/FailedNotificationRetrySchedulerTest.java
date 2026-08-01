package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sagant.distributednotification.config.property.NotificationRetryProperties;
import com.sagant.distributednotification.domain.entity.Notification;
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
    }

    @Test
    void retryFailedNotifications_whenNoneEligible_doesNothing() {
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 1)).thenReturn(List.of());

        scheduler.retryFailedNotifications();

        verifyNoInteractions(notificationProcessingListener);
    }
}
