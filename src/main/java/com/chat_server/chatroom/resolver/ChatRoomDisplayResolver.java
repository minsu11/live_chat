package com.chat_server.chatroom.resolver;

import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;

public interface ChatRoomDisplayResolver {
    String resolveTitle(Long roomId, Long userId);
    String resolveTitle(Long roomId, Long userId, ChatRoom room);

    ChatRoomSummaryResponse resolveSummary(Long roomId, Long userId, ChatRoom room);
}
