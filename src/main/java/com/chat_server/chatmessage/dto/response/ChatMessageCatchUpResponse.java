package com.chat_server.chatmessage.dto.response;

import java.util.List;

public record ChatMessageCatchUpResponse(
        Long roomId,
        List<ChatMessageResponse> messages,
        boolean hasMore,
        Long lastMessageId
) {
}
