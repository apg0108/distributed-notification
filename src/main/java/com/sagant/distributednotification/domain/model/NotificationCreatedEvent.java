package com.sagant.distributednotification.domain.model;

import java.util.UUID;

public record NotificationCreatedEvent(UUID notificationId, String recipient, NotificationChannel channel, String subject, String body,
                                       NotificationPriority priority) {

}
