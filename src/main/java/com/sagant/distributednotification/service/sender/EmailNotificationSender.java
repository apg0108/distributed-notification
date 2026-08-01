package com.sagant.distributednotification.service.sender;

import org.springframework.stereotype.Component;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

@Component
public class EmailNotificationSender implements NotificationSender {

   @Override
   public NotificationChannel getChannel() {
      return NotificationChannel.EMAIL;
   }

   @Override
   public void send(final Notification notification) {
      throw new UnsupportedOperationException("EMAIL channel is not implemented yet");
   }
}
