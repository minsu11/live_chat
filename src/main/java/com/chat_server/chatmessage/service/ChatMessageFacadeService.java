package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;

public interface ChatMessageFacadeService {
    void sendMessage(ChatSendRequest request, Long userId);

    void sendSystemLeaveMessage(Long roomId, Long userId);
}
