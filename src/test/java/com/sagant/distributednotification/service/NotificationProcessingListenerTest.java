package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationCreatedEvent;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationProcessingListener listener;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        listener = new NotificationProcessingListener(notificationRepository);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(NotificationProcessingListener.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(NotificationProcessingListener.class)).detachAppender(logAppender);
    }

    @Test
    void onNotificationCreated_withPendingNotification_logsAndMarksAsSent() {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient("user@example.com");
        notification.setChannel(NotificationChannel.LOG);
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        listener.onNotificationCreated(new NotificationCreatedEvent(id, "user@example.com", NotificationChannel.LOG, "body"));

        assertThat(logAppender.list).anyMatch(event -> event.getFormattedMessage().contains(id.toString()));

        final ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(savedCaptor.getValue().getSentAt()).isNotNull();
    }

    @Test
    void onNotificationCreated_whenNotificationAlreadyProcessed_skipsProcessing() {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        listener.onNotificationCreated(new NotificationCreatedEvent(id, "user@example.com", NotificationChannel.LOG, "body"));

        verify(notificationRepository, never()).save(any());
    }
}
