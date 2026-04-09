package com.chat_server.common.mapper;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatListUpsertEventMapper {
    private final WebSocketProperties webSocketProperties;

    public ChatListUpsertEvent toChatListUpsertEvent(
            Long roomId,
            String displayName,
            Integer unreadCount,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            LocalDateTime orderAt
    ) {
        String eventType = webSocketProperties.getEvent().getChatListUpsert();
        return new ChatListUpsertEvent(
                roomId,
                eventType,
                displayName,
                unreadCount,
                lastMessagePreview,
                lastMessageAt,
                orderAt
        );
    }

    public ChatListUpsertEvent toChatListUpsertEvent(ChatListItemResponse item) {
        return toChatListUpsertEvent(
                item.roomId(),
                item.displayName(),
                item.unreadCount(),
                item.lastMessagePreview(),
                item.lastMessageAt(),
                item.orderAt()
        );
    }
    public ChatListUpsertEvent toChatListUpsertEvent(ChatListItemResponse item, String preview) {
        return toChatListUpsertEvent(
                item.roomId(),
                item.displayName(),
                item.unreadCount(),
                preview,
                item.lastMessageAt(),
                item.orderAt()
        );
    }

}
