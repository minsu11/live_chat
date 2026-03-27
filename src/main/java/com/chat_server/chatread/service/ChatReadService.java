package com.chat_server.chatread.service;

import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.request.ChatReadRequest;

public interface ChatReadService {

    /**
     * 실시간 읽음 요청을 처리한다.
     *
     * @param request 읽음 요청 DTO
     * @param userId 요청 사용자 ID
     * @return 읽음 갱신 이벤트 DTO
     */
    ChatReadUpdatedEvent read(ChatReadRequest request, Long userId);

    /**
     * 채팅방 입장 시 최신 메시지 ID 기준으로 읽음 상태를 갱신한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param latestMessageId 채팅방의 최신 메시지 ID. 메시지가 없으면 null
     */
    void markAsReadOnEnter(Long roomId, Long userId, Long latestMessageId);

}
