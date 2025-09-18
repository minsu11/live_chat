package com.chat_server.user.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import com.chat_server.user.service.UserLoginService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

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
    private final CustomProperties customProperties;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginTokenResponse>> login(@RequestBody LoginRequest user,
                                                                 HttpServletResponse response) {
        log.info("login controller start - request: {}", user);

        ResponseEntity<ApiResponse<LoginTokenResponse>> loginResponseEntity = userLoginService.login(user);
        ApiResponse<LoginTokenResponse> loginResponse = loginResponseEntity.getBody();
        String tokenExpireHeaderName = customProperties.getAuth().getHeader().getTokenExpiration();
        log.info("login controller end - response: {}", loginResponse);
        // 만료시간 헤더 값 파싱
        String expiresIn = loginResponseEntity.getHeaders().getFirst(tokenExpireHeaderName);

        if(Objects.isNull(expiresIn)) {
            log.info("login controller end - expiresIn is null");
            throw new IllegalArgumentException("expiresIn is null");
        }
        ZonedDateTime expiration = ZonedDateTime.parse(expiresIn);
        Duration duration = Duration.between(ZonedDateTime.now(ZoneOffset.UTC), expiration);

        // 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from("accessToken", loginResponse.getData().accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(duration)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        log.info("login controller end - token expires in {}", expiresIn);
        return ResponseEntity.ok(loginResponse);
    }

}
