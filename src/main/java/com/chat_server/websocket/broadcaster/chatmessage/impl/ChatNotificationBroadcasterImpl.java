package com.chat_server.websocket.broadcaster.chatmessage.impl;

import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;
import com.chat_server.redis.service.RedisPublisher;
import com.chat_server.websocket.broadcaster.chatmessage.ChatNotificationBroadcaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationBroadcasterImpl implements ChatNotificationBroadcaster {
    private final RedisPublisher redisPublisher;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void broadcastToUser(Long userId, ChatNotificationEvent event) {
        log.debug("chat notification broadcaster");
        String userDestination =
                webSocketProperties.getSubPrefix()
                +webSocketProperties.getChat().getChatNotification();
        String destination = "/user/"+userId+userDestination;

        redisPublisher.publish(destination,event);

    }
}
