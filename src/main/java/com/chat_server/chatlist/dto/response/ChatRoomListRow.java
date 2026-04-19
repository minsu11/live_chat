package com.chat_server.chatlist.dto.response;


import java.time.LocalDateTime;

public record ChatRoomListRow(Long roomId,
                              String displayName,
                              String lastMessagePreview,
                              LocalDateTime lastMessageAt,
                              Integer unreadCount,
                              LocalDateTime orderAt,
                              boolean muted
) {

}

