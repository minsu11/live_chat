package com.chat_server.chatroom.dto.event;

import java.time.LocalDateTime;

public record ChatRoomSummaryEvent(
        Long roomId,
        String eventType,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        Integer unreadCount
) {
}
