package com.chat_server.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

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

@Getter
@NoArgsConstructor
public class LoginRequest {
    private String userId;
    private String password;
}
