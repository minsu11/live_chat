package com.chat_server.websocket.broadcaster;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;

public interface ChatMessageBroadCaster {
    void broadcastMessage( ChatMessageResponse response);
}
