package com.chat_server.chatlist.dto.event;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 row를 생성/갱신(upsert)하기 위한 WebSocket 이벤트 DTO
 *
 * <p>용도:
 * <ul>
 *   <li>그룹방 생성 직후 chat list에 row 추가</li>
 *   <li>향후 chat list 실시간 갱신 이벤트로 확장 가능</li>
 * </ul>
 *
 * <p>주의:
 * unreadCount는 "채팅방 목록용 unreadCount"이다.
 */
public record ChatListUpsertEvent(
        Long roomId,
        String type,
        String displayName,
        Integer unreadCount,
        LocalDateTime lastMessageAt,
        LocalDateTime orderAt
) {
}