package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void createChatMessage(ChatRoom chatRoom, String messageType, String text) {
        ChatMessage chatMessage = new ChatMessage();

    }
}
