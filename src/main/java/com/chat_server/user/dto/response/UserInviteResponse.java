package com.chat_server.user.dto.response;

public record UserInviteResponse(
        Long userId,
        String nickname
) {
}
