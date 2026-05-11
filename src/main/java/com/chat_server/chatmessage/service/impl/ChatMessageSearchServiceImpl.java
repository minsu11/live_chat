package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatmessage.service.ChatMessageSearchService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageSearchServiceImpl implements ChatMessageSearchService {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public List<ChatMessage> searchMessages(
        Long roomId,
        String keyword,
        LocalDateTime cursorCreatedAt,
        Long cursorMessageId,
        int limit
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 101);

        String rawKeyword = keyword.trim();
        String booleanKeyword = toBooleanModeKeyword(rawKeyword);

        log.info(
            "[MESSAGE_SEARCH] roomId={}, rawKeyword={}, booleanKeyword={}, cursorCreatedAt={}, cursorMessageId={}, limit={}",
            roomId,
            rawKeyword,
            booleanKeyword,
            cursorCreatedAt,
            cursorMessageId,
            safeLimit
        );

        return chatMessageRepository.searchMessagesByKeyword(
            roomId,
            rawKeyword,
            booleanKeyword,
            cursorCreatedAt,
            cursorMessageId,
            safeLimit
        );
    }

    /**
     * MySQL BOOLEAN MODE Full-Text Search용 검색어로 변환한다.
     *
     * 예:
     * - "사과" -> "+사과*"
     * - "아이스티" -> "+아이스티*"
     * - "banana" -> "+banana*"
     * - "ice tea" -> "+ice* +tea*"
     */
    private String toBooleanModeKeyword(String keyword) {
        String normalized = keyword.trim();

        String result = Arrays.stream(normalized.split("\\s+"))
            .map(this::sanitizeBooleanToken)
            .filter(token -> !token.isBlank())
            .map(token -> "+" + token + "*")
            .collect(Collectors.joining(" "));

        return result.isBlank() ? normalized : result;
    }

    /**
     * BOOLEAN MODE 특수문자 제거.
     *
     * 사용자가 입력한 +, -, *, ", (, ) 등이 boolean 연산자로 해석되는 것을 막는다.
     */
    private String sanitizeBooleanToken(String token) {
        return token.replaceAll("[+\\-~*<>@()\"']", "").trim();
    }
}