package com.sagant.distributednotification.domain.model;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(final int status, final String error, final String message) {
        return new ErrorResponse(Instant.now(), status, error, message, List.of());
    }

    public static ErrorResponse of(final int status, final String error, final String message, final List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, fieldErrors);
    }
}
