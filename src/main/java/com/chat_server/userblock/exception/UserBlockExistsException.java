package com.chat_server.userblock.exception;

import com.chat_server.common.dto.exception.ConflictException;
import com.chat_server.error.enumulation.ErrorCode;

public class UserBlockExistsException extends ConflictException {
    /**
     * 이미 차단된 관계 기본 예외를 생성한다.
     */
    public UserBlockExistsException(){
        super(ErrorCode.USER_BLOCK_EXISTS, "user block exist");
    }

    /**
     * 이미 차단된 관계 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 차단 관계 충돌 상세 메시지
     */
    public UserBlockExistsException(String message){
        super(ErrorCode.USER_BLOCK_EXISTS, message);
    }
}
