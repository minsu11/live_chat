package com.chat_server.websocket.broadcaster.chatmessage.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageReadBroadcaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageReadBroadcasterImpl implements ChatMessageReadBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void readRoomBroadcast(Long roomId, ChatReadUpdatedEvent event) {
        String destination = webSocketProperties.getSubPrefix()
            + webSocketProperties.getChat().getRoomPath()
            + "/" + roomId
            + "/read";

        messagingTemplate.convertAndSend(destination, event);
    }
}
