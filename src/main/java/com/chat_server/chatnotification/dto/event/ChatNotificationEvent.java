package com.chat_server.chatnotification.dto.event;

import java.time.LocalDateTime;

public record ChatNotificationEvent(
        Long roomId,
        String eventType,
        String title,
        String preview,
        LocalDateTime createdAt
) {
}
