package com.sagant.distributednotification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sagant.distributednotification.domain.model.NotificationChannel;

class NotificationSenderResolverTest {

    @Test
    void resolve_returnsTheSenderRegisteredForTheChannel() {
        final NotificationSender logSender = Mockito.mock(NotificationSender.class);
        Mockito.when(logSender.getChannel()).thenReturn(NotificationChannel.LOG);

        final NotificationSenderResolver resolver = new NotificationSenderResolver(List.of(logSender));

        assertThat(resolver.resolve(NotificationChannel.LOG)).isSameAs(logSender);
    }

    @Test
    void resolve_whenNoSenderRegisteredForChannel_throws() {
        final NotificationSenderResolver resolver = new NotificationSenderResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(NotificationChannel.EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL");
    }
}
