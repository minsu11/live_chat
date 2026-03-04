package com.chat_server.user.service;

import java.util.Optional;

public interface UserDisplayNameService {
    Optional<String> resolveDisplayName(Long senderId, Long receiverId);
}
