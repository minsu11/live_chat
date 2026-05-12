package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;


public interface ChatMessageSearchService {

    /**
     * 채팅 메시지를 키워드로 검색한다.
     *
     * <p>검색 결과는 최신 메시지부터 반환한다.
     * cursor가 있으면 해당 messageId보다 오래된 메시지만 조회한다.</p>
     *
     * @param roomId          채팅방 ID
     * @param keyword         검색어
     * @param cursorMessageId 다음 페이지 조회 기준 messageId
     * @param limit           조회 개수
     * @return 검색된 메시지 목록
     */
    List<ChatMessage> searchMessages(
        Long roomId,
        String keyword,
        LocalDateTime cursorCreatedAt,
        Long cursorMessageId,
        int limit
    );
}

