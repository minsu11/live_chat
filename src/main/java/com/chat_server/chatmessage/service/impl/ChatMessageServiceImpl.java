package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
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

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * 커서 조건에 맞는 채팅방 메시지 Slice를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param limit 페이지 크기
     * @param cursorKey 커서 키(없으면 첫 페이지)
     * @return 커서 기반 메시지 Slice
     */
    @Override
    @Transactional(readOnly = true)
    public Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId, Long userId, int limit,
                                                                   @Nullable ChatMessageCursorKey cursorKey) {
        return chatMessageRepository.getEnterMessagesByCursor(roomId, userId, limit, cursorKey);
    }
    @Override
    @Transactional(readOnly = true)
    public List<UpdatedMessageUnreadCount> findUpdatedUnreadCounts(Long roomId, Long lastReadMessageId) {
        return chatMessageRepository.findUpdatedUnreadCounts(roomId, lastReadMessageId);
    }

    @Override
    public Slice<ChatMessageItemResponse> getMessagesAfter(Long roomId, Long afterMessageId, int limit) {
        return chatMessageRepository.getMessagesAfter(roomId, afterMessageId, limit);
    }

    /**
     * 채팅 메시지를 생성하여 저장한다.
     *
     * @param chatRoom 채팅방
     * @param userId 발신자 ID
     * @param messageType 메시지 타입
     * @param text 메시지 내용
     * @return 저장된 메시지 엔티티
     *
     * <p>예외 상황:
     * <ul>
     *   <li>userId에 해당하는 유저가 없으면 UserNotFoundException</li>
     * </ul>
     */
    @Override
    public ChatMessage createChatMessage(ChatRoom chatRoom, Long userId, String messageType, String text) {
        log.info("chat message create chat message start");
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        ChatMessage chatMessage = ChatMessage.create(chatRoom, user, text, messageType);
        return chatMessageRepository.save(chatMessage);
    }
}
