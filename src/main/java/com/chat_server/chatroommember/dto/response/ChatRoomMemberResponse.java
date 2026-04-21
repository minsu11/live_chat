package com.chat_server.chatroommember.dto.response;

public record ChatRoomMemberResponse(
        String uuid,
        String nickname,
        String profileUrl,
        boolean isMe
) {
}
