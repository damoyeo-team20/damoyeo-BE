package com.damoyeo.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }
}
