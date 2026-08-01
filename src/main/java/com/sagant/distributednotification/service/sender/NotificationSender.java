package com.sagant.distributednotification.service.sender;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

public interface NotificationSender {

   NotificationChannel getChannel();

   void send(Notification notification);
}
