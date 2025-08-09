package com.chat_server.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchUserRequest(
    @NotBlank
        @Size(min = 1, max = 25)
    String userId) {

}
