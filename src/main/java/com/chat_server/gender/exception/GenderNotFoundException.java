package com.chat_server.gender.exception;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.error.enumulation.ErrorCode;

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
    /**
     * 성별 정보 미존재 기본 예외를 생성한다.
     */
    public GenderNotFoundException() {
        super(ErrorCode.GENDER_NOT_FOUND, "gender not found");
    }

    /**
     * 성별 정보 미존재 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 성별 정보 조회 실패 상세 메시지
     */
    public GenderNotFoundException(String message) {
        super(ErrorCode.GENDER_NOT_FOUND, message);
    }
}
