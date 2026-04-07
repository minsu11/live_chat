package com.chat_server.websocket.broadcaster.chatmessage;

import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;

public interface ChatNotificationBroadcaster {
    void broadcastToUser(Long userId, ChatNotificationEvent event);
}
