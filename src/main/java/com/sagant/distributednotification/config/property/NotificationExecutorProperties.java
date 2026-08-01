package com.sagant.distributednotification.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.executor")
public record NotificationExecutorProperties(int corePoolSize, int maxPoolSize, int queueCapacity) {
}
