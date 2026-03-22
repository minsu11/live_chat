package com.chat_server.error.dto;

import com.chat_server.error.enumulation.ErrorCode;

/**
 * 전역 예외 응답 바디를 표현하는 DTO.
 *
 * @param errorCode 에러 코드
 * @param message 사용자 노출 메시지
 */
public record ErrorResponse(
        ErrorCode errorCode,
        String message
) {
}
