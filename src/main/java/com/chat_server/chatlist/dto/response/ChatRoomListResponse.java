package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatRoomListResponse (
        Long chatRoomId,
        String chatRoomName,
        LocalDateTime lastMessageAt
){

}
