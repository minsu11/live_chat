package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.response.ChatMessageCatchUpResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.common.cursor.CursorKey;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
public class ChatMessageController {

    private final ChatRoomFacadeService chatRoomFacadeService;


    @GetMapping("/{roomId}/messages/after")
    public ResponseEntity<ApiResponse<ChatMessageCatchUpResponse>> getMessagesAfter(
            @PathVariable Long roomId,
            @RequestParam Long afterMessageId,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("getMessagesAfter({}, {}, {}, {})", roomId, afterMessageId, limit);
        Long userId= authenticatedUser.userId();

        ChatMessageCatchUpResponse response =
                chatRoomFacadeService.getMessagesAfter(roomId, userId,afterMessageId, limit);


        return ResponseEntity.ok(
                ApiResponse.success(200,"누락 메시지 복구 조회 성공", response)
        );
    }
}
