package com.chat_server.chatmessage.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ChatMessageItemResponse(
    Long Id,
    Long senderId,
    String senderNickName,
    String content,
    LocalDateTime createdAt
){
}
