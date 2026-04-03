package com.chat_server.user.exception;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.error.enumulation.ErrorCode;

/**
 * packageName    : com.chat_server.user.exception
 * fileName       : UserNotFoundException
 * author         : parkminsu
 * date           : 25. 5. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 27.        parkminsu       최초 생성
 */
public class UserNotFoundException extends NotFoundException {
    /**
     * 사용자 미존재 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 사용자 조회 실패 상세 메시지
     */
    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    /**
     * 사용자 미존재 기본 예외를 생성한다.
     */
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND, "user not found");
    }

    public UserNotFoundException(Long userId) {
    }
}
