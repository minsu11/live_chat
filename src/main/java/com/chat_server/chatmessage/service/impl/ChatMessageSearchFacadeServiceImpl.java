package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchPageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSearchResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageSearchFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.user.service.UserDisplayNameService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageSearchFacadeServiceImpl implements ChatMessageSearchFacadeService {

    private final ChatMessageSearchServiceImpl chatMessageSearchService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserDisplayNameService userDisplayNameService;

    /**
     * 채팅방 메시지를 키워드로 검색한다.
     *
     * <p>검색 방식:
     * <ol>
     *     <li>채팅방 멤버 검증</li>
     *     <li>limit + 1개 조회</li>
     *     <li>조회 결과가 limit보다 크면 hasNext=true</li>
     *     <li>프론트에는 limit개만 반환</li>
     *     <li>마지막 메시지 ID를 nextCursor로 반환</li>
     * </ol>
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @param keyword 검색어
     * @param cursor 다음 페이지 조회 기준 messageId
     * @param limit 페이지 크기
     * @return 검색 결과 페이지
     */
    @Override
    public ChatMessageSearchPageResponse searchChatMessages(
        Long roomId,
        Long userId,
        String keyword,
        String cursor,
        int limit
    ) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return new ChatMessageSearchPageResponse(List.of(), null, false);
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        LocalDateTime cursorCreatedAt = null;
        Long cursorMessageId = null;

        if (cursor != null && !cursor.isBlank()) {
            /**
             * ✅ 중요
             * 아래 decode 부분은 세인님 프로젝트의 실제 ChatMessageCursorCodec 반환 타입에 맞춰서 수정해야 한다.
             *
             * 예를 들어 기존 코드가:
             * ChatMessageCursor decoded = ChatMessageCursorCodec.decode(cursor);
             * decoded.atEpochMillis()
             * decoded.messageId()
             *
             * 이런 구조라면 그대로 쓰면 된다.
             */
            var decoded = ChatMessageCursorCodec.decode(cursor);

            if (decoded != null) {
                cursorCreatedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(decoded.lastMessageAtEpochMillis()),
                    ZoneId.systemDefault()
                );

                cursorMessageId = decoded.lastMessageId();
            }
        }

        // 다음 페이지 존재 여부 확인을 위해 limit + 1개 조회
        List<ChatMessage> foundMessages = chatMessageSearchService.searchMessages(
            roomId,
            keyword,
            cursorCreatedAt,
            cursorMessageId,
            safeLimit + 1
        );

        if (foundMessages.isEmpty()) {
            return new ChatMessageSearchPageResponse(List.of(), null, false);
        }

        boolean hasNext = foundMessages.size() > safeLimit;

        List<ChatMessage> pageMessages = hasNext
            ? foundMessages.subList(0, safeLimit)
            : foundMessages;

        List<Long> senderIds = pageMessages.stream()
            .map(message -> message.getSender().getId())
            .distinct()
            .toList();

        Map<Long, String> displayNameCache =
            userDisplayNameService.resolveDisplayNamesBulk(userId, senderIds);

        List<ChatMessageSearchResponse> items = pageMessages.stream()
            .map(message -> {
                Long senderId = message.getSender().getId();

                String displayNickname = displayNameCache.getOrDefault(
                    senderId,
                    message.getSender().getNickname()
                );

                return ChatMessageSearchResponse.builder()
                    .messageId(message.getId())
                    .roomId(roomId)
                    .messageType(message.getMessageType().name())
                    .senderId(senderId)
                    .senderNickname(displayNickname)
                    .profileImageUrl(null)
                    .content(message.getMessageContent())
                    .createdAt(message.getCreatedAt())
                    .build();
            })
            .toList();

        String nextCursor = null;

        if (hasNext && !pageMessages.isEmpty()) {
            ChatMessage last = pageMessages.get(pageMessages.size() - 1);

            long lastAtEpochMillis = last.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

            /**
             * ✅ 검색 결과의 마지막 메시지를 기준으로 검색용 nextCursor 생성.
             * 채팅방 enter API의 cursor를 가져다 쓰면 안 된다.
             */
            nextCursor = ChatMessageCursorCodec.encode(
                lastAtEpochMillis,
                last.getId()
            );
        }

        return new ChatMessageSearchPageResponse(
            items,
            nextCursor,
            hasNext
        );
    }
}
