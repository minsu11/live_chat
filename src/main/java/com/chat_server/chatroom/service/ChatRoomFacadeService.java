package com.chat_server.chatroom.service;

import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;

public interface ChatRoomFacadeService {
    ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId);

    ChatRoomEnterResponse enterChatRoom(Long roomId, Long userId, String cursor, int limit);

    ChatRoomResult getOrCreateOneToOneChatRoom(Long userId, String friendUuid);
}
