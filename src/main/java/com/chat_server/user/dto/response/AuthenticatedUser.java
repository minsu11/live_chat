package com.chat_server.user.dto.response;

import java.io.Serializable;

public record AuthenticatedUser(Long userId, String role) implements Serializable {
}
