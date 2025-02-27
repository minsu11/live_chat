package com.chat_server.common.dto.exception;

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
public class ConflictException extends RuntimeException {

    public ConflictException() {
        super("conflict");
    }

    public ConflictException(String message) {
        super(message);
    }
}
