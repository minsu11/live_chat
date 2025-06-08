package com.chat_server.user.service.impl;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.adaptor.AuthServerAdaptor;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import com.chat_server.user.service.UserLoginService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * packageName    : com.chat_server.user.service.impl
 * fileName       : UserLoginServiceImpl
 * author         : parkminsu
 * date           : 25. 5. 11.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 11.        parkminsu       최초 생성
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final AuthServerAdaptor authServerAdaptor;

    @Override
    public ApiResponse<LoginTokenResponse> login(LoginRequest loginRequest) {
        // 기본적인 validation은 api server에서 할 예정
        log.info("Login request: {}", loginRequest);
        ApiResponse<LoginTokenResponse> loginTokenResponse = authServerAdaptor.login(loginRequest);
        log.info("Login response: {}", loginTokenResponse);
        return loginTokenResponse;
    }

}
