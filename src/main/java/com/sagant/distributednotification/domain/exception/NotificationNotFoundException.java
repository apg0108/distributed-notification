package com.sagant.distributednotification.domain.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(final UUID id) {
        super("Notification not found: " + id);
    }
}
