package com.chat_server.chatroom.dto.response;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;

import java.util.List;

public record ChatRoomEnterResponse(
    Long roomId,
    String roomType,
    String title,
    List<ChatMessageResponse> messages,
    String nextCursor,
    boolean muted
) {
}
