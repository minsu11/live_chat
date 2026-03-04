package com.chat_server.chatmessageread.service;

import com.chat_server.chatmessage.entity.ChatMessage;

public interface ChatMessageReadService {

    /**
     * 읽은 채팅 메세지 업데이트 기능(현재는 1대1)
     * @param chatRoomId
     * @param userId
     */
    void updateChatMessageRead(long chatRoomId, long userId, ChatMessage chatMessage);
}
