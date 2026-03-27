package com.chat_server.chatread.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 실시간 읽음 처리 요청 DTO.
 *
 * @param roomId 채팅방 ID
 * @param messageId 읽은 메시지 ID
 */
public record ChatReadRequest(
    @NotNull Long roomId,
    @NotNull Long messageId
) {
}
