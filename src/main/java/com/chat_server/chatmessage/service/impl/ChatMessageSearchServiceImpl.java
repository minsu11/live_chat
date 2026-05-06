package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatmessage.service.ChatMessageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageSearchServiceImpl implements ChatMessageSearchService {
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public List<ChatMessage> searchMessages(Long roomId, String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of(); // 검색어가 없으면 빈 리스트 반환
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100); // 최대 100개 제한
        return chatMessageRepository.searchMessagesByKeyword(roomId, keyword.trim(), safeLimit);
    }

}
