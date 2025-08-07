package com.chat_server.user.service;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import org.springframework.http.ResponseEntity;

/**
 * packageName    : com.chat_server.user.service.impl
 * fileName       : UserLoginService
 * author         : parkminsu
 * date           : 25. 5. 11.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 11.        parkminsu       최초 생성
 */
public interface UserLoginService {
    ResponseEntity<ApiResponse<LoginTokenResponse>> login(LoginRequest loginRequest);

}
