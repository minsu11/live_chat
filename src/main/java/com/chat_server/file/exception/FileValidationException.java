package com.chat_server.file.exception;

import com.chat_server.error.enumulation.ErrorCode;
import lombok.Getter;

@Getter
public class FileValidationException extends RuntimeException {
    private final ErrorCode errorCode;

    public FileValidationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
