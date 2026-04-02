package com.chat_server.chatread.dto.event;

import java.util.List;

/**
 * 실시간 읽음 상태 갱신 이벤트.
 *
 * @param roomId 채팅방 ID
 * @param readerUserUuid 읽은 사용자 ID
 * @param lastReadMessageId 마지막 읽은 메시지 ID
 * @param updatedMessages 업데이트되는 메세지
 */
public record ChatReadUpdatedEvent(
    Long roomId,
//    Long readerUserId,
    String readerUserUuid,
    Long lastReadMessageId,
    List<UpdatedMessageUnreadCount> updatedMessages
) {
}