package com.chat_server.chatroom.dto.event;

import java.time.LocalDateTime;

/**
 * 채팅방 목록(chat list) row를 생성/갱신하기 위한 전용 이벤트
 *
 * <p>용도:
 * <ul>
 *   <li>그룹방 생성 직후 각 사용자 chat list에 새 row를 추가</li>
 *   <li>추후 필요하면 chat list upsert 공통 이벤트로도 재사용 가능</li>
 * </ul>
 *
 * <p>주의:
 * <ul>
 *   <li>unreadCount는 "채팅방 목록용 unreadCount"이다.</li>
 *   <li>메시지 버블용 unreadCount와 의미가 다르다.</li>
 * </ul>
 */
public record ChatListUpsertEvent(
    Long roomId,
    String roomType,
    String title,
    String profileUrl,
    String lastMessagePreview,
    LocalDateTime lastMessageAt,
    Integer unreadCount
) {
}