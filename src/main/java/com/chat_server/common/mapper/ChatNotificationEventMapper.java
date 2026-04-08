package com.chat_server.common.mapper;

import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;
import com.chat_server.chatnotification.entity.ChatNotification;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventMapper {
    private final WebSocketProperties webSocketProperties;

    public ChatNotificationEvent toChatNotificationEvent(
            Long roomId,
            String title,
            String preview,
            LocalDateTime createdAt
    ) {
        String eventType = webSocketProperties.getEvent().getChatNotification();

        return new ChatNotificationEvent(
                roomId,
                eventType,
                title,
                preview,
                createdAt
        );
    }
}
