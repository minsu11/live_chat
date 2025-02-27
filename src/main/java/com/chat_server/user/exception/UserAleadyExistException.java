package com.chat_server.user.exception;

import com.chat_server.common.dto.exception.ConflictException;

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
    public UserAleadyExistException() {
        super();
    }

    public UserAleadyExistException(String message) {
        super(message);
    }
}
