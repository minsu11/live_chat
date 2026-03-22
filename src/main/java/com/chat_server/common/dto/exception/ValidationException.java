package com.chat_server.common.dto.exception;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;

/**
 * packageName    : com.chat_server.common.dto.exception
 * fileName       : ValidationException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class ValidationException extends BusinessException {
    /**
     * 요청 데이터 검증 실패 기본 예외를 생성한다.
     */
    public ValidationException() {
        super(ErrorCode.VALIDATION_ERROR, "validation error");
    }

    /**
     * 요청 데이터 검증 실패 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 검증 실패 상세 메시지
     */
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
