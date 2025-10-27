package com.chat_server.chatroom.service;

import com.chat_server.chattype.enumulation.ChatType;

public interface ChatRoomFacadeService {
    void createOneToOneChatRoom(Long userId, String friendUuid);
}
