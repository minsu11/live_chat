package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatRoomListResponse (
    String chatRoomName,
    LocalDateTime lastMessageAt,
    long chatRoomId

){

}
