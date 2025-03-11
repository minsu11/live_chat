package com.chat_server.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * packageName    : com.chat_server.security.dto
 * fileName       : AuthUserDto
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
@Getter
@AllArgsConstructor
public class UserAuthDto {
    private String userInputId;
    private String userInputPassword;
}
