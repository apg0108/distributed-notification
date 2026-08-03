package com.sagant.distributednotification.domain.model;

import java.util.Comparator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationPriority {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    public static final Comparator<NotificationPriority> HIGHEST_FIRST = Comparator.comparingInt(NotificationPriority::getWeight).reversed();

    private final int weight;
}
