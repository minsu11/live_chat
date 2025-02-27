package com.chat_server.gender.exception;

import com.chat_server.common.dto.exception.NotFoundException;

/**
 * packageName    : com.chat_server.gender.exception
 * fileName       : GenderNotFoundException
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public class GenderNotFoundException extends NotFoundException {
    public GenderNotFoundException() {
        super();
    }

    public GenderNotFoundException(String message) {
        super(message);
    }
}
