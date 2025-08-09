package com.chat_server.friend.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserFriendRegisterRequest(@NotBlank
                                        @Size(min = 2, max = 50)
                                        String friendId) {
}
