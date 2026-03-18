package com.chat_server.chatroom.service;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

public interface ChatRoomService {
    // cursor pagenation
    CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit, @Nullable String cursor);

    Long createOneToOneChatRoom(Long userId, Long friendId);

    ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId);

    void updateLastMessageInfo(ChatRoom chatRoom, ChatMessage chatMessage);
}
