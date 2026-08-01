package com.sagant.distributednotification.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sagant.distributednotification.config.property.NotificationExecutorProperties;
import com.sagant.distributednotification.config.property.NotificationRetryProperties;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({ NotificationRetryProperties.class, NotificationExecutorProperties.class })
public class AsyncConfig {

   @Bean(name = "notificationTaskExecutor")
   public Executor notificationTaskExecutor(final NotificationExecutorProperties notificationExecutorProperties) {
      final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(notificationExecutorProperties.corePoolSize());
      executor.setMaxPoolSize(notificationExecutorProperties.maxPoolSize());
      executor.setQueueCapacity(notificationExecutorProperties.queueCapacity());
      executor.setThreadNamePrefix("notification-worker-");
      executor.initialize();
      return executor;
   }
}
