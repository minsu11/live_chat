package com.chat_server.chatlist.controller;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.friend.dto.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat-list")
@RequiredArgsConstructor
public class ChatListController {
    private final ChatListService chatListService;
    // todo 채팅방 목록 불러오기
    // 로그인 할 때 채팅창 목록 불러오기
    @GetMapping()
    public ResponseEntity<ApiResponse<CursorPageResponse<ChatRoomListResponse>>> getChatRoomList(
        @AuthenticationPrincipal(expression = "userId" )Long userId,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String cursor){
        log.info("chat list controller");

        CursorPageResponse<ChatRoomListResponse> cursorPageResponse = chatListService.getChatRoomListsByCursor(userId, limit, cursor);

        ApiResponse<CursorPageResponse<ChatRoomListResponse>> apiResponse = ApiResponse.success(200,cursorPageResponse);
        return ResponseEntity.ok(apiResponse);
    }


}
