package com.chat_server.websocket.broadcaster.chatmessage;

import com.chat_server.chatroom.dto.event.ChatListUpsertEvent;

/**
 * 채팅방 목록(chat list) 갱신 이벤트 전송기
 */
public interface ChatListEventBroadcaster {

    /**
     * 특정 사용자에게 chat list upsert 이벤트를 전송한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  chat list row 생성/갱신 이벤트
     */
    void broadcastToUser(Long userId, ChatListUpsertEvent event);
}
