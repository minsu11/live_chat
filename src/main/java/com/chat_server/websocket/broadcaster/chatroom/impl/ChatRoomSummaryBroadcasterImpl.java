package com.chat_server.websocket.broadcaster.chatroom.impl;

import com.chat_server.chatroom.dto.event.ChatRoomSummaryEvent;
import com.chat_server.redis.service.RedisPublisher;
import com.chat_server.websocket.broadcaster.chatroom.ChatRoomSummaryBroadcaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomSummaryBroadcasterImpl implements ChatRoomSummaryBroadcaster {
    private final RedisPublisher redisPublisher;
    private final WebSocketProperties webSocketProperties;


    @Override
    public void broadcastToUser(Long userId, ChatRoomSummaryEvent event) {
        String userDestination = webSocketProperties.getSubPrefix()
                + webSocketProperties.getChat().getSummaryEventPath();
        String destination = "/user/"+userId+userDestination;
        redisPublisher.publish(destination, event);
    }
}
