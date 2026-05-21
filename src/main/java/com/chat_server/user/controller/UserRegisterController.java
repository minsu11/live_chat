package com.chat_server.user.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.request.InputIdCheckRequest;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.InputIdCheckResponse;
import com.chat_server.user.service.UserFacadeService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("${custom.api.common.prefix}${custom.api.user.prefix}")
@RequiredArgsConstructor
public class UserRegisterController {
    private final UserFacadeService userFacadeService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserRegisterRequest registerRequest) {
        userFacadeService.signUp(registerRequest);
        log.info("회원가입 처리 완료");
        return ResponseEntity.status(201).body(ApiResponse.success(201));
    }

    @PostMapping("input-id/check")
    public ResponseEntity<ApiResponse<InputIdCheckResponse>> checkInputId(
            @Valid @RequestBody InputIdCheckRequest inputIdCheckRequest
    ){

        InputIdCheckResponse inputIdCheckResponse = userFacadeService.checkInputIdAvailability(inputIdCheckRequest);
        ApiResponse<InputIdCheckResponse> response = ApiResponse.success(200, inputIdCheckResponse);
        return ResponseEntity.ok(response);
    }
}
