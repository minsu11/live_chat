package com.chat_server.error.exception;

import com.chat_server.error.enumulation.ErrorCode;
import lombok.Getter;

/**
 * ErrorCode 기반 전역 예외 처리를 위해 사용하는 비즈니스 예외.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode 이름을 메시지로 사용하는 비즈니스 예외를 생성한다.
     *
     * @param errorCode 예외 의미/HTTP 상태를 나타내는 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 사용자 노출 메시지를 함께 가지는 비즈니스 예외를 생성한다.
     *
     * @param errorCode 예외 의미/HTTP 상태를 나타내는 코드
     * @param message 사용자/로그에 전달할 상세 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
