package com.chat_server.chatroom.service;

public interface ChatReadService {

    /**
     * 채팅방 입장 시 최신 메시지 ID 기준으로 읽음 상태를 갱신한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param latestMessageId 채팅방의 최신 메시지 ID. 메시지가 없으면 null
     */
    void markAsReadOnEnter(Long roomId, Long userId, Long latestMessageId);
}
