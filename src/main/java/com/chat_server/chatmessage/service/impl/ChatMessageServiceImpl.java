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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Transactional(readOnly = true)
    @Override
    public List<UpdatedMessageUnreadCount> calculateUnreadCountsWithRedis(
            Long roomId,
            Long currentMessageId,
            Map<Long, Long> memberReadMap,
            int totalMemberCount
    ) {
        // 1. 화면에 보일 만한 최근 메시지 50개만 조회 (성능 최적화)
        // 50개면 한 화면의 말풍선 숫자를 모두 갱신하기에 충분합니다.
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(
                roomId,
                currentMessageId,
                PageRequest.of(0, 50)
        );

        // 2. 각 메시지마다 안읽은 사람 수를 계산합니다.
        return recentMessages.stream().map(msg -> {
            Long msgId = msg.getId();

            // memberReadMap의 value(마지막 읽은 ID)가 이 메시지 ID(msgId)보다 크거나 같으면 읽은 사람!
            long readCount = memberReadMap.values().stream()
                    .filter(lastReadId -> lastReadId >= msgId)
                    .count();

            // 안읽은 사람 수 = (전체 인원 - 1(본인 제외)) - 읽은 사람 수
            int unreadCount = (int) Math.max(0, totalMemberCount - readCount);

            return new UpdatedMessageUnreadCount(msgId, unreadCount);
        }).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatMessage> getContextMessages(Long roomId, Long targetMessageId, int halfLimit) {
        // 1. 과거 메시지 (타겟 포함)
        List<ChatMessage> olderMessages = chatMessageRepository.findOlderMessagesWithTarget(roomId, targetMessageId, halfLimit);

        // 2. 미래 메시지 (타겟 미포함)
        List<ChatMessage> newerMessages = chatMessageRepository.findNewerMessages(roomId, targetMessageId, halfLimit);

        // 3. olderMessages는 내림차순(DESC)으로 가져왔으므로, 합치기 전에 오름차순(ASC)으로 뒤집어줍니다.
        List<ChatMessage> combined = new ArrayList<>();
        for (int i = olderMessages.size() - 1; i >= 0; i--) {
            combined.add(olderMessages.get(i));
        }

        // 4. 미래 메시지 추가 (이미 ASC 정렬됨)
        combined.addAll(newerMessages);

        return combined;
    }
}
