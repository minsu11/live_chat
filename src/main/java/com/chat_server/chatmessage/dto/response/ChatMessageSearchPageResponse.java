package com.chat_server.chatmessage.dto.response;


import java.util.List;

/**
 * 채팅 메시지 검색 결과 페이지 응답 DTO.
 *
 * <p>검색 결과는 최신 메시지부터 내려간다.
 * nextCursor는 다음 검색 페이지를 조회할 때 사용할 cursor 값이다.</p>
 *
 * @param items 검색 결과 목록
 * @param nextCursor 다음 페이지 cursor. 다음 페이지가 없으면 null
 * @param hasNext 다음 페이지 존재 여부
 */
public record ChatMessageSearchPageResponse(
    List<ChatMessageSearchResponse> items,
    String nextCursor,
    boolean hasNext
) {
}