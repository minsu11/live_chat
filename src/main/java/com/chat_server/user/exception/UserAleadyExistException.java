package com.chat_server.user.exception;

import com.chat_server.common.dto.exception.ConflictException;
import com.chat_server.error.enumulation.ErrorCode;

/**
 * packageName    : com.chat_server.user.exception
 * fileName       : UserAleadyExistException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class UserAleadyExistException extends ConflictException {
    /**
     * 사용자 중복(이미 존재) 기본 예외를 생성한다.
     */
    public UserAleadyExistException() {
        super(ErrorCode.USER_ALREADY_EXISTS, "user already exists");
    }

    /**
     * 사용자 중복(이미 존재) 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 중복 원인 상세 메시지
     */
    public UserAleadyExistException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }
}
