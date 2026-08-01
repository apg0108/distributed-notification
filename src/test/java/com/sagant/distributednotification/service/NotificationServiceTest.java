package com.sagant.distributednotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.mapper.NotificationMapper;
import com.sagant.distributednotification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotification_returnsResponseMappedFromTheSavedEntity() {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.LOG, "subject", "body",
                NotificationPriority.HIGH, Map.of("k", "v"));

        final Notification entityFromMapper = new Notification();
        entityFromMapper.setRecipient("user@example.com");
        entityFromMapper.setChannel(NotificationChannel.LOG);
        entityFromMapper.setBody("body");

        final Notification savedEntity = new Notification();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setRecipient("user@example.com");
        savedEntity.setChannel(NotificationChannel.LOG);
        savedEntity.setBody("body");
        savedEntity.setStatus(NotificationStatus.PENDING);

        final NotificationResponse expectedResponse = new NotificationResponse(
                savedEntity.getId(), "user@example.com", NotificationChannel.LOG, "subject", "body",
                NotificationPriority.HIGH, Map.of("k", "v"), NotificationStatus.PENDING);

        when(notificationMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(notificationRepository.save(entityFromMapper)).thenReturn(savedEntity);
        when(notificationMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        final NotificationResponse actualResponse = notificationService.createNotification(request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void createNotification_invokesMapperAndRepositoryInOrder_withNoExtraCalls() {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.EMAIL, null, "body", null, null);
        final Notification entityFromMapper = new Notification();
        final Notification savedEntity = new Notification();
        savedEntity.setId(UUID.randomUUID());
        final NotificationResponse response = new NotificationResponse(
                savedEntity.getId(), "user@example.com", NotificationChannel.EMAIL, null, "body",
                NotificationPriority.MEDIUM, null, NotificationStatus.PENDING);

        when(notificationMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(notificationRepository.save(entityFromMapper)).thenReturn(savedEntity);
        when(notificationMapper.toResponse(savedEntity)).thenReturn(response);

        notificationService.createNotification(request);

        final InOrder inOrder = Mockito.inOrder(notificationMapper, notificationRepository);
        inOrder.verify(notificationMapper).toEntity(request);
        inOrder.verify(notificationRepository).save(entityFromMapper);
        inOrder.verify(notificationMapper).toResponse(savedEntity);
        verifyNoMoreInteractions(notificationMapper, notificationRepository);
    }
}
