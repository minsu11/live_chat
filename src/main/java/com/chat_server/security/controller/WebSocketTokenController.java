package com.chat_server.security.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ws")
public class WebSocketTokenController {

    @GetMapping("/token")
    public ResponseEntity<ApiResponse<String>> getToken(HttpServletRequest request) {
        log.info("get token controller");
        String accessToken = CookieUtil.getCookie(request, "accessToken");
        ApiResponse<String> response = ApiResponse.success(200,"access token 반환", accessToken);
        log.info("token: {}", accessToken);
        return ResponseEntity.ok(response);
    }

}
