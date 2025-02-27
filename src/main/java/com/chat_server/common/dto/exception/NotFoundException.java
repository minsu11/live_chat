package com.chat_server.common.dto.exception;

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
public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super("not found");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
