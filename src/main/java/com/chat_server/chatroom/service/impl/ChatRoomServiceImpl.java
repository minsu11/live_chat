package com.chat_server.chatroom.service.impl;

import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.user.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    public CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        // room list는 대화 목록이 있는 경우만 대화 목록에 끌고 오게 하기
        return null;
    }

    @Override
    public void createChatRoom(String userId, String friendId) {
        // chat room 미리 만들기
        // 대화창 중복 관리를 위한 hash string 만들기(1:1 대화방에서만 생성)

    }
}
