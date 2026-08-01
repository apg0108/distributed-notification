package com.sagant.distributednotification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

class EmailNotificationSenderTest {

    private final EmailNotificationSender sender = new EmailNotificationSender();

    @Test
    void getChannel_returnsEmail() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void send_throwsBecauseSmtpIsNotImplementedYet() {
        assertThatThrownBy(() -> sender.send(new Notification())).isInstanceOf(UnsupportedOperationException.class);
    }
}
