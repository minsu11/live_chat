package com.chat_server.chatmessage.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ChatMessageResponse (
    Long messageId,
    Long roomId,
    Long senderId,
    String senderNickName,
    String content,
    LocalDateTime createdAt
){
}
