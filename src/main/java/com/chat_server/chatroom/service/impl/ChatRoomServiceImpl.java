package com.chat_server.chatroom.service.impl;

import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    @Override
    public CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        return null;
    }
}
