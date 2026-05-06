package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.entity.ChatMessage;

import java.util.List;

public interface ChatMessageSearchService {

    List<ChatMessage> searchMessages(Long roomId, String keyword, int limit);
}
