package com.sagant.distributednotification.service.sender;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.sagant.distributednotification.config.property.NotificationEmailProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

   private final JavaMailSender mailSender;

   private final NotificationEmailProperties notificationEmailProperties;

   @Override
   public NotificationChannel getChannel() {
      return NotificationChannel.EMAIL;
   }

   @Override
   public void send(final Notification notification) {
      final SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(notificationEmailProperties.from());
      message.setTo(notification.getRecipient());
      message.setSubject(notification.getSubject() != null ? notification.getSubject() : "");
      message.setText(notification.getBody());
      mailSender.send(message);
   }
}
