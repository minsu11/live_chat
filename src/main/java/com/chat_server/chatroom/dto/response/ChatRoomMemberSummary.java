package com.chat_server.chatroom.dto.response;

public record ChatRoomMemberSummary(
        String userUuid,
        String nickname,
        String profileImageUrl
) {
}