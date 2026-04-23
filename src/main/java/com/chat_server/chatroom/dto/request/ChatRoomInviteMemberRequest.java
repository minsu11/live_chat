package com.chat_server.chatroom.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ChatRoomInviteMemberRequest(
        @NotNull List<String> memberUuids
) {
}
