package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatListItemResponse(
        Long roomId,
        String displayName,
        Integer unreadCount,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime orderAt
) {
}
