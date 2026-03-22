package com.chat_server.chatmessage.dto.response;

public record ChatMessageSenderResponse(
    Long senderId,
    String senderNickname
) {
}
