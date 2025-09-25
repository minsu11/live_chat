package com.chat_server.user.dto.response;

/**
 * packageName    : com.chat_server.user.dto.response
 * fileName       : LoginTokenResponse
 * author         : parkminsu
 * date           : 25. 5. 11.
 * description    : auth 서버에서 응답 받는 token dto, access token만 응답 받음.
 * refresh token auth 서버에서 바로 redis 저장할 예정
 * <p>
 * =========================================================== </br>
 * DATE              AUTHOR             NOTE </br>
 * ----------------------------------------------------------- </br>
 * 25. 5. 11.        parkminsu       최초 생성 </br>
 */
public record LoginTokenResponse(String accessToken, String accessTokenExpiration) {
}
