package com.sagant.distributednotification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.mapper.NotificationMapper;
import com.sagant.distributednotification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

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
