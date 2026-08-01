package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationCreatedEvent;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;
import com.sagant.distributednotification.service.sender.NotificationSender;
import com.sagant.distributednotification.service.sender.NotificationSenderResolver;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSenderResolver notificationSenderResolver;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private NotificationProcessingListener listener;

    @Test
    void onNotificationCreated_withPendingNotification_dispatchesAndMarksAsSent() {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient("user@example.com");
        notification.setChannel(NotificationChannel.LOG);
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationSenderResolver.resolve(NotificationChannel.LOG)).thenReturn(notificationSender);

        listener.onNotificationCreated(
                new NotificationCreatedEvent(id, "user@example.com", NotificationChannel.LOG, "subject", "body", NotificationPriority.MEDIUM));

        verify(notificationSender).send(notification);

        final ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(savedCaptor.getValue().getSentAt()).isNotNull();
        assertThat(MDC.get(NotificationProcessingListener.NOTIFICATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void onNotificationCreated_whenNotificationAlreadyProcessed_skipsProcessing() {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        listener.onNotificationCreated(
                new NotificationCreatedEvent(id, "user@example.com", NotificationChannel.LOG, "subject", "body", NotificationPriority.MEDIUM));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void onNotificationCreated_whenSenderFails_marksAsFailedWithoutRetryingInline() {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationSenderResolver.resolve(NotificationChannel.EMAIL)).thenReturn(notificationSender);
        doThrow(new UnsupportedOperationException("EMAIL channel is not implemented yet")).when(notificationSender).send(notification);

        listener.onNotificationCreated(
                new NotificationCreatedEvent(id, "user@example.com", NotificationChannel.EMAIL, "subject", "body", NotificationPriority.MEDIUM));

        final ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(savedCaptor.getValue().getRetryCount()).isZero();
        assertThat(savedCaptor.getValue().getLastError()).isEqualTo("EMAIL channel is not implemented yet");
    }
}
