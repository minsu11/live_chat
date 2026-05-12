package com.chat_server.error.enumulation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_MISSING(HttpStatus.UNAUTHORIZED),
    NO_PERMISSION(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    GENDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_DEFINE(HttpStatus.INTERNAL_SERVER_ERROR),
    CONFLICT(HttpStatus.CONFLICT),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
    USER_BLOCK_EXISTS(HttpStatus.CONFLICT),
    INVALID_INPUT(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_UPLOAD(HttpStatus.PAYLOAD_TOO_LARGE),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND)
    ;

    private final HttpStatus status;

    /**
     * 에러 코드 enum 항목과 HTTP 상태를 매핑한다.
     *
     * @param status 해당 에러 코드의 HTTP 상태
     */
    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    /**
     * 문자열 코드를 {@link ErrorCode}로 안전하게 변환한다.
     *
     * <p>예외 상황:
     * <ul>
     *   <li>코드가 null/blank 이거나 enum에 없는 값이면 {@link #NOT_DEFINE}을 반환한다.</li>
     * </ul>
     *
     * @param code 변환 대상 문자열 코드
     * @return 변환된 ErrorCode 또는 NOT_DEFINE
     */
    public static ErrorCode from(String code) {
        if (code == null || code.isBlank()) {
            return NOT_DEFINE;
        }

        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return NOT_DEFINE;
        }
    }
}
