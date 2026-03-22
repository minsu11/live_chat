package com.chat_server.userprofile.exception;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.error.enumulation.ErrorCode;

public class UserProfileNotFoundException extends NotFoundException {
    /**
     * 사용자 프로필 미존재 기본 예외를 생성한다.
     */
    public UserProfileNotFoundException(){
        super(ErrorCode.USER_PROFILE_NOT_FOUND, "user profile not found");
    }

    /**
     * 사용자 프로필 미존재 예외를 상세 메시지와 함께 생성한다.
     *
     * @param message 프로필 조회 실패 상세 메시지
     */
    public UserProfileNotFoundException(String message){
        super(ErrorCode.USER_PROFILE_NOT_FOUND, message);
    }

}
