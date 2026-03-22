package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.service.ChatListFacadeService;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.friend.dto.response.CursorPageResponse;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatListFacadeServiceImpl implements ChatListFacadeService {
    private final ChatListService chatListService;


    @Override
    public CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit, @Nullable String cursor) {
        return null;
    }

    @Override
    public void ensureMembership(Long roomId, Long userId) {

    }

    @Override
    public void increaseUnreadCount(Long roomId, Long senderId) {

    }
}
