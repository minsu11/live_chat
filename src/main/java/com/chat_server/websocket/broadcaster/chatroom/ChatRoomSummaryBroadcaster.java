package com.chat_server.websocket.broadcaster.chatroom;

import com.chat_server.chatroom.dto.event.ChatRoomSummaryEvent;

public interface ChatRoomSummaryBroadcaster {
    void broadcastToUser(Long userId, ChatRoomSummaryEvent event);
}
