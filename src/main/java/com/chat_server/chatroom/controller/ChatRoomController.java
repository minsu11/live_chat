package com.chat_server.chatroom.controller;


import com.chat_server.chatroom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-room")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // todo 채팅방 생성

    // todo 채팅방 나가기(채팅방 삭제)

    // todo 채팅방 설정 업데이트(해당 업데이트는 실제로 업데이트 할 예정)


}
