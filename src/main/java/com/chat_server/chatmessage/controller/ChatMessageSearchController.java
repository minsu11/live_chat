package com.chat_server.chatmessage.controller;
import com.chat_server.chatmessage.dto.response.ChatMessageSearchResponse;
import com.chat_server.chatmessage.service.impl.ChatMessageSearchFacadeServiceImpl;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
public class ChatMessageSearchController {

    private final ChatMessageSearchFacadeServiceImpl chatMessageSearchFacadeService;

    @GetMapping("/{roomId}/messages/search")
    public ResponseEntity<ApiResponse<List<ChatMessageSearchResponse>>> searchMessages(
            @PathVariable Long roomId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser // Security 설정에 따라 어노테이션 변경
    ) {
        log.info("search message start: {}", keyword);
        Long userId = authenticatedUser.userId();
        List<ChatMessageSearchResponse> response =
                chatMessageSearchFacadeService.searchChatMessages(roomId, userId, keyword, limit);
        ApiResponse<List<ChatMessageSearchResponse>> apiResponse = ApiResponse.success(200,"검색 결과 완료", response);
        return ResponseEntity.ok(apiResponse);
    }
}