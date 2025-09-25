package com.chat_server.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * packageName    : com.chat_server.user.dto.request
 * fileName       : LoginRequest
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */

public record LoginRequest(@NotBlank String userId, @NotBlank String password) {

}
