package com.chat_server.chatroomsetting.dto.response;

public record ChatRoomNameUpdateResponse(
        Long roomId,
        String displayName
) {
    public static ChatRoomNameUpdateResponse of(Long roomId, String customRoomName) {
        return new ChatRoomNameUpdateResponse(roomId, customRoomName);
    }
}
