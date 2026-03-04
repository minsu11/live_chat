package com.chat_server.chatmessageread.service.impl;


import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessageread.entity.ChatMessageRead;
import com.chat_server.chatmessageread.exception.ChatMessageReadNotFoundException;
import com.chat_server.chatmessageread.repository.ChatMessageReadRepository;
import com.chat_server.chatmessageread.service.ChatMessageReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageReadServiceImpl implements ChatMessageReadService {

    private final ChatMessageReadRepository chatMessageReadRepository;

    @Override
    public void updateChatMessageRead(long chatRoomId, long userId, ChatMessage chatMessage) {
        ChatMessageRead chatMessageRead = chatMessageReadRepository.findByChatRoomIdAndUserId(chatRoomId,userId)
            .orElseThrow(ChatMessageReadNotFoundException::new);
        chatMessageRead.updateChatMessage(chatMessage);
    }
}
