package com.sagant.distributednotification.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.retry")
public record NotificationRetryProperties(int maxAttempts) {

}
