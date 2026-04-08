package com.chat_server.common.mapper;

import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatReadUpdatedEventMapper {
    private final WebSocketProperties webSocketProperties;

    public ChatReadUpdatedEvent toChatReadUpdatedEvent(
            Long roomId,
            String readerUserUuid,
            Long lastReadMessageId,
            List<UpdatedMessageUnreadCount> updatedMessageUnreadCounts
    ) {
        String messageType=  webSocketProperties.getEvent().getMessageRead();
        return new ChatReadUpdatedEvent(
                roomId,
                messageType,
                readerUserUuid,
                lastReadMessageId,
                updatedMessageUnreadCounts
        );
    }
}
