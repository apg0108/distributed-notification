package com.sagant.distributednotification.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.security")
public record ApiKeyProperties(String apiKey) {
}
