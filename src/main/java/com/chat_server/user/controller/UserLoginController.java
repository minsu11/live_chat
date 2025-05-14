package com.chat_server.user.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import com.chat_server.user.service.UserLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * packageName    : com.chat_server.user.controller
 * fileName       : UserLoginController
 * author         : parkminsu
 * date           : 25. 5. 9.
 * description    : login controller
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 9.        parkminsu       최초 생성
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserLoginController {

    private final UserLoginService userLoginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginTokenResponse>> login(@RequestBody LoginRequest user) {
        log.info("login controller start");
        log.info("Login request: {}", user);
        ApiResponse<LoginTokenResponse> loginTokenResponse = userLoginService.login(user);
        log.debug("Login response: {}", loginTokenResponse);
        log.info("login controller end");
        return ResponseEntity.ok(loginTokenResponse);

    }
}
