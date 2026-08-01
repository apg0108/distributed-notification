package com.sagant.distributednotification.service.sender;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogNotificationSender implements NotificationSender {

   private final ObjectMapper objectMapper;

   @Override
   public NotificationChannel getChannel() {
      return NotificationChannel.LOG;
   }

   @Override
   public void send(final Notification notification) {
      final Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("notificationId", notification.getId());
      payload.put("recipient", notification.getRecipient());
      payload.put("channel", notification.getChannel());
      payload.put("subject", notification.getSubject());
      payload.put("body", notification.getBody());
      payload.put("priority", notification.getPriority());

      try {
         log.info(objectMapper.writeValueAsString(payload));
      } catch (final JsonProcessingException ex) {
         throw new IllegalStateException("Failed to serialize notification " + notification.getId() + " for LOG channel", ex);
      }
   }
}
