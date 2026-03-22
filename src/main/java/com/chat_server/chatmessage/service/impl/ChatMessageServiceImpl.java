package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId, int limit,
                                                                   @Nullable ChatMessageCursorKey cursorKey) {
        return chatMessageRepository.getEnterMessagesByCursor(roomId, limit, cursorKey);
    }

    @Override
    public ChatMessage createChatMessage(ChatRoom chatRoom, Long userId, String messageType, String text) {
        log.info("chat message create chat message start");
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        ChatMessage chatMessage = ChatMessage.create(chatRoom, user, text, messageType);
        return chatMessageRepository.save(chatMessage);
    }
}
