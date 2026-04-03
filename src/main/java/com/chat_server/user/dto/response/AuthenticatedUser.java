package com.chat_server.user.dto.response;

import java.io.Serializable;
import java.security.Principal;

public record AuthenticatedUser(Long userId, String role) implements Serializable, Principal {
    @Override
    public String getName(){
        return String.valueOf(userId);
    }
}
