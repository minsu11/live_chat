package com.chat_server.error.enumulation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    NO_PERMISSION(HttpStatus.FORBIDDEN),
    NOT_DEFINE(HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public static ErrorCode from(String code) {
        if (code == null || code.isBlank()) {
            return NOT_DEFINE;
        }

        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return NOT_DEFINE;
        }
    }
}