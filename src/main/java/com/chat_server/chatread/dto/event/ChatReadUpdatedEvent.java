package com.chat_server.chatread.dto.event;
/**
 * 실시간 읽음 상태 갱신 이벤트.
 *
 * @param roomId 채팅방 ID
 * @param readerUserId 읽은 사용자 ID
 * @param lastReadMessageId 마지막 읽은 메시지 ID
 */
public record ChatReadUpdatedEvent(
    Long roomId,
    Long readerUserId,
    Long lastReadMessageId
) {
}