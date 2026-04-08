package com.chat_server.chatroom.dto.response;

import java.util.List;

public record CreateChatRoomResponse(
        Long roomId,
        String roomType,
        String title
) {
}
