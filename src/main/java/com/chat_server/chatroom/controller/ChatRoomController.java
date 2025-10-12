package com.chat_server.chatroom.controller;


import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // todo 채팅방 목록 가지고 오기


    // todo 채팅방 생성
    @GetMapping("{userId}/register")
    public ResponseEntity<ApiResponse<Void>> createChatRoom(
            @PathVariable String userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){

        log.info("chat room register");

        return null;
    }

    // todo 채팅방 나가기(채팅방 삭제)

    // todo 채팅방 설정 업데이트(해당 업데이트는 실제로 업데이트 할 예정)


}
