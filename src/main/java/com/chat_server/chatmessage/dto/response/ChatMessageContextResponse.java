package com.chat_server.chatmessage.dto.response;
import java.util.List;

public record ChatMessageContextResponse(
        Long roomId,
        List<ChatMessageResponse> messages,
        String prevCursor,
        String nextCursor
) {
}
