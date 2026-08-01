package com.sagant.distributednotification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.sagant.distributednotification.config.property.NotificationEmailProperties;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new EmailNotificationSender(mailSender, new NotificationEmailProperties("notifications@sagant.local"));
    }

    @Test
    void getChannel_returnsEmail() {
        assertThat(sender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void send_sendsAnEmailWithTheNotificationContent() {
        final Notification notification = new Notification();
        notification.setRecipient("user@example.com");
        notification.setSubject("subject");
        notification.setBody("body");

        sender.send(notification);

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        final SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("notifications@sagant.local");
        assertThat(sentMessage.getTo()).containsExactly("user@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("subject");
        assertThat(sentMessage.getText()).isEqualTo("body");
    }

    @Test
    void send_whenSubjectIsNull_usesEmptySubject() {
        final Notification notification = new Notification();
        notification.setRecipient("user@example.com");
        notification.setBody("body");

        sender.send(notification);

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEmpty();
    }
}
