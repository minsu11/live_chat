package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.common.cursor.ChatListCursorCodec;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.friend.dto.response.CursorPageResponse;
import jakarta.annotation.Nullable;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatListServiceImpl implements ChatListService {
    private final ChatListRepository chatListRepository;

    @Override
    @Transactional(readOnly = true)
    /**
     * 커서 기반 채팅방 목록을 조회한다.
     *
     * <p>동작:
     * <ul>
     *   <li>입력 커서를 디코딩한다.</li>
     *   <li>레포지토리에서 {@code limit + 1} 형태의 slice를 조회해 hasNext를 판단한다.</li>
     *   <li>마지막 요소 기준으로 next cursor를 인코딩한다.</li>
     * </ul>
     *
     * @param userId 조회 대상 사용자 ID
     * @param limit 페이지 크기
     * @param cursor 현재 페이지 커서(없으면 첫 페이지)
     * @return 커서 페이지 응답
     */
    public CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        // 1) 커서 디코드 (없거나 깨졌으면 null 반환되어 첫 페이지로 처리됨)
        ChatListCursorKey decoded = ChatListCursorCodec.decode(cursor);

        // 2) 레포지토리에서 +1건 조회 (hasNext 판별용)
        Slice<ChatRoomListResponse> slice =
            chatListRepository.getChatRoomListByCursor(userId, limit, decoded);

        // 3) next 커서 생성
        String next = null;
        log.info("chat list : {}", slice.toString());
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatRoomListResponse last = slice.getContent().get(slice.getContent().size() - 1);

            long lastAtEpochMillis = last.lastMessageAt()
                .atOffset(ZoneOffset.UTC)   // DB를 UTC 기준 LocalDateTime으로 본다는 가정
                .toInstant()
                .toEpochMilli();

            next = ChatListCursorCodec.encode(lastAtEpochMillis, last.roomId());
        }

        // 4) 공통 응답 래핑
        return new CursorPageResponse<>(slice.getContent(), next, slice.hasNext());
    }

    @Override
    /**
     * 채팅방-사용자 멤버십을 upsert 한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    public void ensureMembership(Long roomId, Long userId) {
        chatListRepository.upsertMembership(roomId, userId);
    }

    @Override
    /**
     * 발신자를 제외한 멤버의 unread count를 증가시킨다.
     *
     * @param roomId unread 증가 대상 채팅방 ID
     * @param senderId 발신자 ID
     */
    public void increaseUnreadCount(Long roomId, Long senderId) {
        // 메시지 송신 시 발신자를 제외한 사용자 unread count를 증가시킨다.
        log.info("increaseUnreadCount 호출");
        log.debug("increaseUnreadCount params - roomId: {}, senderId: {}", roomId, senderId);
        chatListRepository.increaseUnreadCount(roomId, senderId);
        log.debug("increaseUnreadCount 완료 - roomId: {}, senderId: {}", roomId, senderId);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * 채팅방 멤버 사용자 ID 목록을 조회한다.
     *
     * @param roomId 대상 채팅방 ID
     * @return 멤버 사용자 ID 리스트(없으면 빈 리스트)
     */
    public List<Long> getRoomMemberUserIds(Long roomId) {
        // 채팅방 브로드캐스트 대상 사용자 목록을 조회한다.
        log.info("getRoomMemberUserIds 호출");
        log.debug("getRoomMemberUserIds params - roomId: {}", roomId);
        List<Long> userIds = chatListRepository.findUserIdsByRoomId(roomId);
        log.debug("getRoomMemberUserIds return - userIds: {}", userIds);
        return userIds;
    }
}
