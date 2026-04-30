package com.chat_server.user.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserDisplayNameService {
    Optional<String> resolveDisplayName(Long senderId, Long receiverId);

    Map<Long, String> resolveDisplayNamesBulk(Long senderId, List<Long> receiverIds);
}
