package com.chat_server.chatroom.service;

import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

public interface ChatRoomService {
    // cursor pagenation
    CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit, @Nullable String cursor);

}
