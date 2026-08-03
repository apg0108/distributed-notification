package com.sagant.distributednotification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class NotificationPriorityTest {

    @Test
    void weights_orderPrioritiesFromHighestToLowest() {
        assertThat(NotificationPriority.HIGH.getWeight()).isGreaterThan(NotificationPriority.MEDIUM.getWeight());
        assertThat(NotificationPriority.MEDIUM.getWeight()).isGreaterThan(NotificationPriority.LOW.getWeight());
    }

    @Test
    void highestFirst_sortsByDescendingWeight() {
        final List<NotificationPriority> sorted = Stream
                .of(NotificationPriority.LOW, NotificationPriority.HIGH, NotificationPriority.MEDIUM)
                .sorted(NotificationPriority.HIGHEST_FIRST)
                .toList();

        assertThat(sorted).containsExactly(NotificationPriority.HIGH, NotificationPriority.MEDIUM, NotificationPriority.LOW);
    }

    @Test
    void names_areStableForTextPersistence() {
        assertThat(Stream.of(NotificationPriority.values()).map(Enum::name)).containsExactlyInAnyOrder("HIGH", "MEDIUM", "LOW");
    }
}
