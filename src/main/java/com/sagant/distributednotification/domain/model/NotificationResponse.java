package com.sagant.distributednotification.domain.model;

import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, String recipient, NotificationChannel channel, String subject, String body, NotificationPriority priority,
                                   Map<String, String> metadata, NotificationStatus status) {

}
