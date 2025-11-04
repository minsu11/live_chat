package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatRoomListResponse (
        Long chatroomId,
        String chatRoomName,
        LocalDateTime lastMessageAt
){

}
