package com.chat_server.common.dto.response;

import com.chat_server.common.dto.message.ApiMessageEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

/**
 * packageName    : com.chat_server.common.dto.response
 * fileName       : ApiRsponse
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값인 필드는 응답에서 제외
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<T>(status, message, data);
    }

    public static <T> ApiResponse<T> success(int status, String message) {
        return success(status, message, null);
    }

    public static <T> ApiResponse<T> success(int status) {
        return success(status, ApiMessageEnum.성공.getMessage(), null);
    }

    public static <T> ApiResponse<T> success(int status, T data) {
        return success(status, ApiMessageEnum.성공.getMessage(), data);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<T>(status, message, null);
    }
    public static <T> ApiResponse<T> error(int status) {
        return error(status, ApiMessageEnum.에러.getMessage());
    }
}
