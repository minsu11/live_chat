package com.chat_server.redis.dto;

import java.time.LocalDateTime;

public record ChatRoomMetaDto(
        Long lastMessageId,
        String lastPreview,
        LocalDateTime lastMessageAt
) {
}
