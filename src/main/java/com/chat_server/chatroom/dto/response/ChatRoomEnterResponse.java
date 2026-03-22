package com.chat_server.chatroom.dto.response;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import java.util.List;

public record ChatRoomEnterResponse(
    Long roomId,
    String roomType,
    String title,
    List<ChatMessageItemResponse> messages,
    String nextCursor
) {
}
