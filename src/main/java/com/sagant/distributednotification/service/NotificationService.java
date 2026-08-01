package com.sagant.distributednotification.service;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.exception.NotificationNotFoundException;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.mapper.NotificationMapper;
import com.sagant.distributednotification.repository.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse createNotification(final NotificationRequest request) {
        final Notification notification = notificationMapper.toEntity(request);
        final Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }
}
