package com.chat_server.common.exception;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;

/**
 * packageName    : com.chat_server.common.exception
 * fileName       : AccessTokenMissingException
 * author         : parkminsu
 * date           : 25. 5. 22.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 22.        parkminsu       최초 생성
 */

public class AccessTokenMissingException extends BusinessException {
    /**
     * Access Token 누락 기본 예외를 생성한다.
     */
    public AccessTokenMissingException() {
        super(ErrorCode.ACCESS_TOKEN_MISSING, "token missing");
    }

    /**
     * Access Token 누락 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 누락 원인 상세 메시지
     */
    public AccessTokenMissingException(String message) {
        super(ErrorCode.ACCESS_TOKEN_MISSING, message);
    }
}
