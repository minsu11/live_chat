package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchResponse;

import java.util.List;

public interface ChatMessageSearchFacadeService {

    List<ChatMessageSearchResponse> searchChatMessages(Long roomId, Long userId, String keyword, int limit);
}
