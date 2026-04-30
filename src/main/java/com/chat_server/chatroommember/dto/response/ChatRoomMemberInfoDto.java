package com.chat_server.chatroommember.dto.response;

public record ChatRoomMemberInfoDto(
        Long userId,
        String uuid,
        String nickname,
        String profileUrl
) {
}
