package com.chat_server.common.mapper;

import com.chat_server.chatroom.dto.event.ChatRoomSummaryEvent;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomSummaryEventMapper {
    private final WebSocketProperties webSocketProperties;

    public ChatRoomSummaryEvent toEvent(Long roomId,
                                     String lastMessagePreview,
                                     LocalDateTime lastMessageAt,
                                     Integer unreadCount
                                     ) {
        String messageType = webSocketProperties.getEvent().getRoomSummaryUpdated();
        return new ChatRoomSummaryEvent(
                roomId,
                messageType,
                lastMessagePreview,
                lastMessageAt,
                unreadCount
        );
    }
}
