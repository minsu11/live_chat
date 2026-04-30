package com.chat_server.chatlist.dto.response;

public record ChatRoomNotificationUpdateResponse(
        Long roomId,
        boolean muted
) {
    public static ChatRoomNotificationUpdateResponse of(Long roomId, boolean muted){
        return new ChatRoomNotificationUpdateResponse(roomId, muted);
    }
}
