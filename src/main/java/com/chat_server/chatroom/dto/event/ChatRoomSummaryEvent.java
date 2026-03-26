package com.chat_server.chatroom.dto.event;
public record ChatRoomSummaryEvent(
        String type,
        Long roomId,
        String lastMessagePreview,
        String lastMessageAtDisplay,
        Integer unreadCount
) {
}
