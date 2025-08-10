package com.chat_server.chatlist.service;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

public interface ChatListService {
    CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit, @Nullable String cursor);

}
