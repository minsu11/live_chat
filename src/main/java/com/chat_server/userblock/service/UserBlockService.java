package com.chat_server.userblock.service;

public interface UserBlockService {
    void validateSenderNotBlocked(Long senderUserId, Long receiverUserId);
}
