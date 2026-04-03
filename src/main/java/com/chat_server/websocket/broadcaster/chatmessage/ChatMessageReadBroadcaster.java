package com.chat_server.websocket.broadcaster.chatmessage;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;

public interface ChatMessageReadBroadcaster {
    void readRoomBroadcast(Long roomId, ChatReadUpdatedEvent event);
}
