package com.chat_server.websocket.broadcaster;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;

public interface ChatMessageBroadCaster {
    /**
     * 특정 사용자 전용 destination으로 메시지를 전송한다.
     *
     * @param receiverUserId 수신자 사용자 ID
     * @param response 전송할 채팅 응답 payload
     */
    void broadcastMessage(Long receiverUserId, ChatMessageResponse response);

    /**
     * 룸 구독 destination으로 브로드캐스트를 릴레이한다.
     *
     * @param response 릴레이할 채팅 응답 payload
     */
    void relayRoomBroadcast(ChatMessageResponse response);
}
