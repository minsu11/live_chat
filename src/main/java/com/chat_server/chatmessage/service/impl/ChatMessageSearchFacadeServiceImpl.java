package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageSearchFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.user.service.UserDisplayNameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageSearchFacadeServiceImpl implements ChatMessageSearchFacadeService {

    private final ChatMessageSearchServiceImpl chatMessageSearchService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserDisplayNameService userDisplayNameService;

    public List<ChatMessageSearchResponse> searchChatMessages(Long roomId, Long userId, String keyword, int limit) {
        // 1. 방 참여 권한 검증 (세인님 기존 구조 활용)
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        // 2. FTS 기반 메시지 검색
        List<ChatMessage> foundMessages = chatMessageSearchService.searchMessages(roomId, keyword, limit);

        if (foundMessages.isEmpty()) {
            return List.of();
        }

        // 3. 발송자 ID 추출 및 커스텀 닉네임 일괄 조회 (N+1 방지)
        List<Long> senderIds = foundMessages.stream()
                .map(msg -> msg.getSender().getId())
                .distinct()
                .toList();

        Map<Long, String> displayNameCache = userDisplayNameService.resolveDisplayNamesBulk(userId, senderIds);

        // 4. DTO 변환 (최신순 정렬 상태 그대로 유지)
        return foundMessages.stream()
                .map(msg -> {
                    Long senderId = msg.getSender().getId();
                    String displayNickname = displayNameCache.getOrDefault(senderId, msg.getSender().getNickname());

                    return ChatMessageSearchResponse.builder()
                            .messageId(msg.getId())
                            .roomId(roomId)
                            .messageType(msg.getMessageType().name())
                            .senderId(senderId)
                            .senderNickname(displayNickname)
                            .profileImageUrl(null) // 필요시 프로필 URL 서비스 연동
                            .content(msg.getMessageContent())
                            .createdAt(msg.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
