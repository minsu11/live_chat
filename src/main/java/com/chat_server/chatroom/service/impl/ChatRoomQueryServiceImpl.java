package com.chat_server.chatroom.service.impl;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomQueryServiceImpl implements ChatRoomQueryService {
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public ChatRoom getRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(ChatRoomNotFoundException::new);
    }

    @Override
    public Long getMemberId(Long roomId, Long userId) {

        return chatRoomRepository.findMemberIdByRoomId(roomId,userId)
            .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public void validateMemberOrThrow(Long roomId, Long userId) {
        if(!chatRoomRepository.existsByUserId(roomId,userId)){
            throw new UserNotFoundException();
        }
    }
}
