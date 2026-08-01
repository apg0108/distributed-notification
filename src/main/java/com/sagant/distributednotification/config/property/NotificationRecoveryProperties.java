package com.sagant.distributednotification.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.recovery")
public record NotificationRecoveryProperties(long timeoutMs) {
}
