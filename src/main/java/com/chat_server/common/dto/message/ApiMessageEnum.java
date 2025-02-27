package com.chat_server.common.dto.message;

/**
 * packageName    : com.chat_server.common.dto.message
 * fileName       : ApiMessageEnum
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
public enum ApiMessageEnum {
    성공("데이터 응답에 성공 했습니다."),
    에러("데이터 응답에 실패 했습니다.");

    private String message;

    ApiMessageEnum(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
