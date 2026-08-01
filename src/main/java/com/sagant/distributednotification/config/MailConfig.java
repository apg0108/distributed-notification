package com.sagant.distributednotification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sagant.distributednotification.config.property.NotificationEmailProperties;

@Configuration
@EnableConfigurationProperties(NotificationEmailProperties.class)
public class MailConfig {
}
