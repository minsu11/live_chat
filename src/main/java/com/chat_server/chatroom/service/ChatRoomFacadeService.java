package com.chat_server.chatroom.service;

import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chattype.enumulation.ChatType;

public interface ChatRoomFacadeService {
    ChatRoomSummaryResponse getChatRoomSummary(Long roomId);
    void createOneToOneChatRoom(Long userId, String friendUuid);
}
