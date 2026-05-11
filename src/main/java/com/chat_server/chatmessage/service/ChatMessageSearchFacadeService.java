package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchPageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSearchResponse;

import java.util.List;

public interface ChatMessageSearchFacadeService {

    /**
     * 채팅방 메시지를 키워드로 검색한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @param keyword 검색어
     * @param cursor 다음 페이지 조회 기준 messageId
     * @param limit 페이지 크기
     * @return 검색 결과 페이지
     */
    ChatMessageSearchPageResponse searchChatMessages(
        Long roomId,
        Long userId,
        String keyword,
        String cursor,
        int limit
    );}
