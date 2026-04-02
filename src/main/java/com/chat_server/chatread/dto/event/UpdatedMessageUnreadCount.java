package com.chat_server.chatread.dto.event;

public record UpdatedMessageUnreadCount(
        Long messageId,
        Integer unreadCount
) {
}
