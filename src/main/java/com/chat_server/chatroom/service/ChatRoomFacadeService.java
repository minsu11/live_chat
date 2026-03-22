package com.chat_server.chatroom.service;

import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;

public interface ChatRoomFacadeService {
    ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId);
    ChatRoomResult getOrCreateOneToOneChatRoom(Long userId, String friendUuid);
}
