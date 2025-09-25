package com.chat_server.friend.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.friend.dto.request.UserFriendRegisterRequest;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.friend.service.FriendService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("${custom.api.friend.prefix}")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 친구 목록 불러오기
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<UserFriendResponse>>> getFriends(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,                         // 실제로는 인증정보에서 추출 권장
            @RequestParam(defaultValue = "2") int limit,
            @RequestParam(required = false) @Nullable String cursor
    ) {
        log.info("friend controller start");
        log.info("cursor: {}", cursor);

        CursorPageResponse<UserFriendResponse> cursorPageResponse =friendService.getFriendsByCursor(authenticatedUser.userId(), limit, cursor);
        ApiResponse<CursorPageResponse<UserFriendResponse>> response = ApiResponse.success(200,"친구 목록을 반환합니다.", cursorPageResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody UserFriendRegisterRequest registerRequest
    ){
        // 친구 저장하는 로직
        log.info("friend controller start");
        log.info("user id: {}", authenticatedUser.userId());
        friendService.saveFriend(registerRequest, authenticatedUser.userId());

        ApiResponse<Object> response = ApiResponse.success(201,"저장에 성공했습니다.");
        return ResponseEntity.status(201).body(response);
    }
//     친구 삭제




}
