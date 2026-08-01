package com.sagant.distributednotification.service.sender;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sagant.distributednotification.domain.model.NotificationChannel;

@Component
public class NotificationSenderResolver {

   private final Map<NotificationChannel, NotificationSender> sendersByChannel;

   public NotificationSenderResolver(final List<NotificationSender> senders) {
      this.sendersByChannel = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, Function.identity()));
   }

   public NotificationSender resolve(final NotificationChannel channel) {
      final NotificationSender sender = sendersByChannel.get(channel);
      if (sender == null) {
         throw new IllegalStateException("No NotificationSender registered for channel " + channel);
      }
      return sender;
   }
}
