package com.chat_server.user.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * packageName    : com.chat_server.user.controller
 * fileName       : UserRegisterController
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
@Slf4j
@RestController
@RequestMapping("${custom.api.prefix}/users")
@RequiredArgsConstructor
public class UserRegisterController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserRegisterRequest registerRequest,
                                                BindingResult bindingResult) {
        log.info("controller 시작");
        log.info("register user : {}", registerRequest);
        if(bindingResult.hasErrors()) {
            throw new ValidationException("request validation error");
        }

        userService.signUp(registerRequest);
        log.info("회원가입 처리 완료");
        return ResponseEntity.status(201).body(ApiResponse.success(201));
    }
}
