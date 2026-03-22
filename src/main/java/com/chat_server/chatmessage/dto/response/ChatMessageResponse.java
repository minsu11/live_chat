package com.chat_server.chatmessage.dto.response;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long messageId,
    Long roomId,
    Long senderId,
    String senderNickname,
    String content,
    LocalDateTime createdAt
) {
}
