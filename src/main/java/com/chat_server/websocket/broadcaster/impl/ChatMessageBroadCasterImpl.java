package com.chat_server.websocket.broadcaster.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.websocket.broadcaster.ChatMessageBroadCaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadCasterImpl implements ChatMessageBroadCaster {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;


    @Override
    public void broadcastMessage( ChatMessageResponse response) {
        String payload = webSocketProperties.getSubPrefix()
            + webSocketProperties.getChat().getRoomPath()
            +"/" + response.roomId();
        // Api 공통 응답이 있는데 그걸 사용해야하나?
        messagingTemplate.convertAndSend(payload, response);
    }
}
