package com.chat_server.user.exception;

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
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException() {

        super("user not found");
    }
}
