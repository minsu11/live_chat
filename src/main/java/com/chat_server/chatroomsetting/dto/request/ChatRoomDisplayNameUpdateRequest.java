package com.chat_server.chatroomsetting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRoomDisplayNameUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 50)
        String displayName
) {
}
