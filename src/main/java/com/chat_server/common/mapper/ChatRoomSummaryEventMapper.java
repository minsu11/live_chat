package com.chat_server.common.mapper;

import com.chat_server.chatroom.dto.event.ChatRoomSummaryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ChatRoomSummaryEventMapper {
    private static final String ROOM_SUMMARY_UPDATED = "ROOM_SUMMARY_UPDATED";

    public ChatRoomSummaryEvent toEvent(Long roomId,
                                     String lastMessagePreview,
                                     LocalDateTime lastMessageAt,
                                     Integer unreadCount
                                     ) {
        return new ChatRoomSummaryEvent(
                roomId,
                ROOM_SUMMARY_UPDATED,
                lastMessagePreview,
                lastMessageAt,
                unreadCount
        );
    }
}
