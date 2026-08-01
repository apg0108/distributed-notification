package com.sagant.distributednotification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationPriority;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class LogNotificationSenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LogNotificationSender sender = new LogNotificationSender(objectMapper);

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(LogNotificationSender.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(LogNotificationSender.class)).detachAppender(logAppender);
    }

    @Test
    void getChannel_returnsLog() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.LOG);
    }

    @Test
    void send_logsTheNotificationAsJson() throws Exception {
        final UUID id = UUID.randomUUID();
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient("user@example.com");
        notification.setChannel(NotificationChannel.LOG);
        notification.setSubject("subject");
        notification.setBody("body");
        notification.setPriority(NotificationPriority.HIGH);

        sender.send(notification);

        assertThat(logAppender.list).hasSize(1);
        final JsonNode json = objectMapper.readTree(logAppender.list.get(0).getFormattedMessage());
        assertThat(json.get("notificationId").asText()).isEqualTo(id.toString());
        assertThat(json.get("recipient").asText()).isEqualTo("user@example.com");
        assertThat(json.get("channel").asText()).isEqualTo("LOG");
        assertThat(json.get("subject").asText()).isEqualTo("subject");
        assertThat(json.get("body").asText()).isEqualTo("body");
        assertThat(json.get("priority").asText()).isEqualTo("HIGH");
    }
}
