package com.chat_server.common.dto.exception;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;

/**
 * packageName    : com.chat_server.common.dto.exception
 * fileName       : NotFoundException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class NotFoundException extends BusinessException {
    /**
     * 일반 리소스 미존재(NOT_FOUND) 기본 예외를 생성한다.
     */
    public NotFoundException() {
        super(ErrorCode.NOT_FOUND, "not found");
    }

    /**
     * 일반 리소스 미존재(NOT_FOUND) 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 미존재 원인 설명 메시지
     */
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 하위 도메인 예외에서 사용할 커스텀 404 코드를 받아 예외를 생성한다.
     *
     * @param errorCode 미존재 계열 도메인 에러 코드
     * @param message 상세 메시지
     */
    protected NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
