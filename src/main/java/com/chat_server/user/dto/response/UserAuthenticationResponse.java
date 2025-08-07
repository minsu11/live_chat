package com.chat_server.user.dto.response;

import com.chat_server.user.domain.UserStatus;

/**
 * packageName    : com.chat_server.user.dto.response
 * fileName       : UserReponse
 * author         : parkminsu
 * date           : 25. 5. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 27.        parkminsu       최초 생성
 */

public record UserAuthenticationResponse(UserStatus userStatus) {
}
