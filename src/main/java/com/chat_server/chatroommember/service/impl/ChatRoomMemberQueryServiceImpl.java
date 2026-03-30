package com.chat_server.chatroommember.service.impl;

import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.chatroommember.service.ChatRoomMemberQueryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomMemberQueryServiceImpl implements ChatRoomMemberQueryService {
    private final ChatListRepository chatListRepository;

    @Override
    public List<Long> getMemberUserIds(Long roomId) {
        return chatListRepository.findUserIdsByRoomId(roomId);
    }
}
