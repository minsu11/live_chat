package com.chat_server.userstatus.exception;

/**
 * packageName    : com.chat_server.userstatus.exception
 * fileName       : UserStatusNotFoundException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class UserStatusNotFoundException extends RuntimeException {
    public UserStatusNotFoundException() {
        super();
    }

    public UserStatusNotFoundException(String message) {
        super(message);
    }
}
