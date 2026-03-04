package com.chat_server.chatmessage.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record ChatMessageResponse (
    Long messageId,
    Long roomId,
    Long senderId,
    String senderNickName,
    String message,
    LocalDate createdAt
){
}
