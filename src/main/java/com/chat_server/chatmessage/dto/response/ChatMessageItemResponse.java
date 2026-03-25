package com.chat_server.chatmessage.dto.response;

import java.time.LocalDateTime;

public record ChatMessageItemResponse(
        Long messageId,
        Long senderId,
        String senderUuid,
        String senderNickname,
        String profileImageUrl,
        String messageType,
        String content,
        LocalDateTime createdAt
) {

}
