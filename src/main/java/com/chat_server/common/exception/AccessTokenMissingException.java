package com.chat_server.common.exception;

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

public class AccessTokenMissingException extends RuntimeException {
    public AccessTokenMissingException() {
        super("token missing");
    }

    public AccessTokenMissingException(String message) {
        super(message);
    }
}
