package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.enums.MessageType;

public interface ChatMessageFacadeService {
    void sendMessage(ChatSendRequest request, Long userId);

    void saveAndBroadcastSystemMessage(Long roomId, Long userId, MessageType messageType, String content);
}
