package com.chat_server.user.service;

import com.chat_server.security.dto.UserPrincipal;
import com.chat_server.user.dto.request.UserRegisterRequest;

/**
 * packageName    : com.chat_server.user.service
 * fileName       : UserService
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
public interface UserService {

    void signUp(UserRegisterRequest registerRequest);

    UserPrincipal loadUserByUserId(String userId);

}
