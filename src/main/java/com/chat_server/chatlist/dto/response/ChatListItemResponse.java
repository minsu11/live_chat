package com.chat_server.chatlist.dto.response;

import java.time.LocalDateTime;

public record ChatListItemResponse(Long roomId,
                                   String roomType,
                                   String title,
                                   String profileUrl,
                                   String lastMessagePreview,
                                   LocalDateTime lastMessageAt,
                                   Integer unreadCount,
                                   Integer memberCount) {

}
