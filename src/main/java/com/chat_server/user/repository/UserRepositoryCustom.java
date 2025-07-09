package com.chat_server.user.repository;

import com.chat_server.user.dto.response.AuthorizationUserResponse;
import com.chat_server.user.dto.response.UserAuthenticationResponse;

import java.util.Optional;

/**
 * packageName    : com.chat_server.user.repository
 * fileName       : UserRepositoryCustom
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public interface UserRepositoryCustom {
    Optional<UserAuthenticationResponse> getUserByUserId(String userId);
    Optional<AuthorizationUserResponse> authorizeUserByUserId(String userId, String roleName);
}

