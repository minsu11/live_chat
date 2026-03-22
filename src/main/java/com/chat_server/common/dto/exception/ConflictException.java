package com.chat_server.common.dto.exception;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;

/**
 * packageName    : com.chat_server.common.dto
 * fileName       : ConfictException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class ConflictException extends BusinessException {

    /**
     * 일반 충돌(CONFLICT) 기본 예외를 생성한다.
     */
    public ConflictException() {
        super(ErrorCode.CONFLICT, "conflict");
    }

    /**
     * 일반 충돌(CONFLICT) 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 충돌 원인 설명 메시지
     */
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    /**
     * 하위 도메인 예외에서 사용할 커스텀 충돌 코드를 받아 예외를 생성한다.
     *
     * @param errorCode 충돌 계열 도메인 에러 코드
     * @param message 상세 메시지
     */
    protected ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
