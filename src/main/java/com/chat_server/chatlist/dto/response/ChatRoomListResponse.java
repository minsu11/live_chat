package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatRoomListResponse (
        Long roomId,
        String roomName,
        Integer unreadCount,
        LocalDateTime lastMessageAt
){

}
