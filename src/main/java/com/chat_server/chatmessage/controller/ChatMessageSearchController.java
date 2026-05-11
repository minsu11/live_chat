package com.chat_server.chatmessage.controller;
import com.chat_server.chatmessage.dto.response.ChatMessageSearchPageResponse;
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
    /**
     * 채팅방 메시지를 키워드로 검색한다.
     *
     * <p>검색 결과는 최신 메시지부터 반환된다.
     * cursor는 이전 검색 결과의 마지막 messageId를 의미한다.</p>
     *
     * <p>예시:
     * <ul>
     *     <li>첫 검색: /{roomId}/messages/search?keyword=안녕&limit=30</li>
     *     <li>다음 검색: /{roomId}/messages/search?keyword=안녕&cursor=120&limit=30</li>
     * </ul>
     *
     * @param roomId 채팅방 ID
     * @param keyword 검색어
     * @param cursor 다음 페이지 조회 기준 messageId
     * @param limit 페이지 크기
     * @param authenticatedUser 인증 유저
     * @return 검색 결과 페이지
     */
    @GetMapping("/{roomId}/messages/search")
    public ResponseEntity<ApiResponse<ChatMessageSearchPageResponse>> searchMessages(
        @PathVariable Long roomId,
        @RequestParam String keyword,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int limit,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        log.info("search message start. roomId={}, keyword={}, cursor={}, limit={}",
            roomId, keyword, cursor, limit);

        Long userId = authenticatedUser.userId();

        ChatMessageSearchPageResponse response =
            chatMessageSearchFacadeService.searchChatMessages(
                roomId,
                userId,
                keyword,
                cursor,
                limit
            );

        ApiResponse<ChatMessageSearchPageResponse> apiResponse =
            ApiResponse.success(200, "검색 결과 조회 성공", response);

        return ResponseEntity.ok(apiResponse);
    }
}