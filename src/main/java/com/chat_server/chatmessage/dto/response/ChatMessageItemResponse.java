package com.chat_server.chatmessage.dto.response;

import com.chat_server.chatmessage.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageItemResponse(
    Long messageId,
    Long senderId,
    String senderNickname,
    String messageType,
    String content,
    LocalDateTime createdAt
) {
    public static ChatMessageItemResponse from(ChatMessage chatMessage) {
        return new ChatMessageItemResponse(
            chatMessage.getId(),
            chatMessage.getSender().getId(),
            chatMessage.getSender().getNickname(),
            chatMessage.getMessageType().name(),
            chatMessage.getMessageContent(),
            chatMessage.getCreatedAt()
        );
    }
}
