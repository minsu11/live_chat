package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListRow;
import com.chat_server.chatlist.dto.response.ChatUnreadCountRow;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import com.chat_server.common.cursor.ChatListCursorCodec;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;
import com.chat_server.friend.dto.response.CursorPageResponse;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatListServiceImpl implements ChatListService {
    private final ChatListRepository chatListRepository;
    private final ChatRoomDisplayResolver chatRoomDisplayResolver;
    private final CustomProperties customProperties;

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
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        // 1) 커서 디코드 (없거나 깨졌으면 null 반환되어 첫 페이지로 처리됨)
        ChatListCursorKey decoded = ChatListCursorCodec.decode(cursor);

        // 2) 레포지토리에서 +1건 조회 (hasNext 판별용)
        Slice<ChatRoomListResponse> slice =
            chatListRepository.getChatRoomListByCursor(userId, limit, decoded);

        List<ChatRoomListResponse> content = slice.getContent().stream()
                .map(item -> new ChatRoomListResponse(
                        item.roomId(),
                        chatRoomDisplayResolver.resolveTitle(item.roomId(), userId),
                        item.unreadCount(),
                        item.lastMessagePreview(),
                        item.lastMessageAt(),
                        item.orderAt(),
                        item.muted()
                ))
                .toList();
        // 3) next 커서 생성
        String next = null;
        log.info("chat list : {}", slice.toString());
        if (slice.hasNext() && !content.isEmpty()) {
            ChatRoomListResponse last = content.get(content.size() - 1);
            LocalDateTime cursorBase = last.orderAt();
            if (cursorBase == null) {
                throw new IllegalStateException(
                    "chat list next cursor 생성 실패: orderAt is null. roomId=" + last.roomId()
                );
            }
            long lastAtEpochMillis = cursorBase
                .atOffset(ZoneOffset.UTC)   // DB를 UTC 기준 LocalDateTime으로 본다는 가정
                .toInstant()
                .toEpochMilli();

            next = ChatListCursorCodec.encode(lastAtEpochMillis, last.roomId());
        }

        // 4) 공통 응답 래핑
        return new CursorPageResponse<>(content, next, slice.hasNext());
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

    /**
     * 채팅방 멤버 사용자 ID 목록을 조회한다.
     *
     * @param roomId 대상 채팅방 ID
     * @return 멤버 사용자 ID 리스트(없으면 빈 리스트)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> getRoomMemberUserIds(Long roomId) {
        // 채팅방 브로드캐스트 대상 사용자 목록을 조회한다.
        log.info("getRoomMemberUserIds 호출");
        log.debug("getRoomMemberUserIds params - roomId: {}", roomId);
        List<Long> userIds = chatListRepository.findUserIdsByRoomId(roomId);
        log.debug("getRoomMemberUserIds return - userIds: {}", userIds);
        return userIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getCustomRoomName(Long roomId, Long userId) {
        return chatListRepository.findCustomNameByUserIdAndRoomId(roomId, userId);
    }

    /**
     * 채팅방 입장 시 last_read_message_id와 unread_count를 갱신한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param messageId 최신 메시지 ID
     * @return 업데이트된 row 수
     */
    @Override
    public int markAsReadOnEnter(Long roomId, Long userId, Long messageId) {
        return chatListRepository.markAsReadOnEnter(
                roomId,
                userId,
                messageId,
                LocalDateTime.now()
        );
    }

    /**
     * 메시지가 없는 채팅방 입장 시 unread_count만 초기화한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 업데이트된 row 수
     */
    @Override
    public int clearUnreadCountOnEnter(Long roomId, Long userId) {
        return chatListRepository.clearUnreadCountOnEnter(
                roomId,
                userId,
                LocalDateTime.now()
        );
    }

    /**
     * 메시지 전송 시 발신자 본인의 chat_list를 읽은 상태로 맞춘다.
     *
     * @param roomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param messageId 저장된 메시지 ID
     * @return 업데이트된 row 수
     */
    @Override
    public int markSenderAsReadOnSend(Long roomId, Long senderId, Long messageId) {
        return chatListRepository.markSenderAsReadOnSend(
                roomId,
                senderId,
                messageId,
                LocalDateTime.now()
        );
    }
    @Override
    public int markAsRead(Long roomId, Long userId, Long messageId) {
        log.debug("markAsRead start");
        return chatListRepository.markAsRead(
            roomId,
            userId,
            messageId,
            LocalDateTime.now()
        );
    }

    /**
     * 특정 채팅방에서 여러 사용자의 unread count를 조회한다.
     *
     * <p>조회되지 않은 사용자도 결과 맵에는 0으로 채워 넣는다.
     *
     * @param roomId 채팅방 ID
     * @param userIds 사용자 ID 목록
     * @return key=userId, value=unreadCount 맵
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getUnreadCountMap(Long roomId, List<Long> userIds) {
        Map<Long, Integer> result = new LinkedHashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (Long userId : userIds) {
            result.put(userId, 0);
        }

        List<ChatUnreadCountRow> rows = chatListRepository.findUnreadCountRows(roomId, userIds);

        for (ChatUnreadCountRow row : rows) {
            result.put(row.userId(), row.unreadCount() == null ? 0 : row.unreadCount());
        }

        return result;
    }

    @Override
    public boolean getMuted(Long roomId, Long userId) {
        return chatListRepository.findMutedByChatRoomIdAndUserId(roomId,userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,customProperties.getError().getMessage(ErrorCode.NOT_FOUND)));

    }

    @Override
    public ChatList updateCustomRoomName(Long userId, Long roomId, String newName) {
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, customProperties.getError().getMessage(ErrorCode.NOT_FOUND)));
        chatList.updateCustomName(newName);
        log.info("update end");
        return chatList;
    }

    @Override
    public ChatList updateMutedStatus(Long roomId, Long userId, boolean muted) {
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId,userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,customProperties.getError().getMessage(ErrorCode.NOT_FOUND)));
        chatList.updateMuted(muted);
        return chatList;
    }

    @Override
    public void leaveChatRoom(Long roomId, Long userId) {
        chatListRepository.deleteByChatRoomIdAndUserIdDirectly(roomId, userId);

        log.info("[Leave Room] User {} left Room {}", userId, roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ChatRoomListRow> getChatListItemsBulk(Long userId, Long roomId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ChatRoomListRow> items = chatListRepository.findChatListItemsBulk(roomId, userIds);

        return items.stream()
                .collect(Collectors.toMap(ChatRoomListRow::userId, item -> item));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatListItemResponse getChatListItem(Long roomId, Long userId) {
        ChatListItemResponse item = chatListRepository.findChatListItem(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "채팅방 목록 row를 찾을 수 없습니다. roomId=" + roomId + ", userId=" + userId
                ));
        return new ChatListItemResponse(
                item.roomId(),
                chatRoomDisplayResolver.resolveTitle(item.roomId(), userId),
                item.unreadCount(),
                item.lastMessagePreview(),
                item.lastMessageAt(),
                item.orderAt()
        );
    }
}
