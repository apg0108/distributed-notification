package com.sagant.distributednotification.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
public record NotificationEmailProperties(String from) {
}
