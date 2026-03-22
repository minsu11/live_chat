package com.chat_server.chatroommember.service.impl;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroommember.entity.ChatRoomMember;
import com.chat_server.chatroommember.enums.RoomMemberRole;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.user.entity.User;
import com.chat_server.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomMemberServiceImpl implements ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public void ensureMembership(Long userId, Long chatRoomId) {
        chatRoomMemberRepository.upsertMembership(chatRoomId,userId,RoomMemberRole.MEMBER.name());
    }

}
