package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatReadUpdatedEventMapper;
import com.chat_server.user.service.UserService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageReadBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatReadFacadeServiceImpl implements ChatReadFacadeService {
    private final ChatReadService chatReadService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatListService chatListService;
    private final ChatMessageReadBroadcaster chatMessageReadBroadcaster;
    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final ChatListEventBroadcaster chatListEventBroadcaster;
    private final ChatReadUpdatedEventMapper chatReadUpdatedEventMapper;
    private final ChatListUpsertEventMapper chatListUpsertEventMapper;

    @Override
    public void read(ChatReadRequest request, Long userId) {
        Long roomId = request.roomId();
        Long messageId = request.messageId();

        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        chatReadService.markAsRead(roomId, userId, messageId);

        broadcastMessageReadUpdatedEvent(roomId, userId, messageId);
        broadcastChatListUpsertEvent(roomId, userId);

    }

    @Override
    public void markAsReadOnEnter(Long roomId, Long userId, Long latestMessageId) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        chatReadService.markAsReadOnEnter(roomId, userId, latestMessageId);

        if (latestMessageId == null) {
            return;
        }

        broadcastMessageReadUpdatedEvent(roomId, userId, latestMessageId);
        broadcastChatListUpsertEvent(roomId, userId);
    }

    /**
     * 메시지 unreadCount 갱신 이벤트를 room 기준으로 broadcast 한다.
     *
     * <p>이 이벤트는 메시지 버블에 표시되는 unreadCount 갱신용이다.
     * 모든 참여자에게 동일한 값이 보여야 하므로 room 기준 broadcast 한다.</p>
     *
     * @param roomId 채팅방 ID
     * @param userId 읽은 사용자 ID
     * @param messageId 읽음 처리 기준 메시지 ID
     */
    private void broadcastMessageReadUpdatedEvent(Long roomId, Long userId, Long messageId) {
        String readerUserUuid = userService.getUuidByUserId(userId);

        List<UpdatedMessageUnreadCount> updatedMessageUnreadCounts =
                chatMessageService.findUpdatedUnreadCounts(roomId, messageId);

        ChatReadUpdatedEvent event = chatReadUpdatedEventMapper.toChatReadUpdatedEvent(
                roomId,
                readerUserUuid,
                messageId,
                updatedMessageUnreadCounts
        );

        chatMessageReadBroadcaster.readRoomBroadcast(roomId, event);
    }

    /**
     * chat list unreadCount 갱신 이벤트를 user 기준으로 broadcast 한다.
     *
     * <p>이 이벤트는 채팅방 목록 row의 unreadCount 갱신용이다.
     * 사용자별 unreadCount 값이 다를 수 있으므로 user 기준 개별 전송한다.</p>
     *
     * @param roomId 채팅방 ID
     * @param userId 읽은 사용자 ID
     */
    private void broadcastChatListUpsertEvent(Long roomId, Long userId) {
        ChatListItemResponse item = chatListService.getChatListItem(roomId, userId);

        ChatListUpsertEvent event = chatListUpsertEventMapper.toChatListUpsertEvent(
                item.roomId(),
                item.displayName(),
                item.unreadCount(),
                item.lastMessagePreview(),
                item.lastMessageAt(),
                item.orderAt()
        );

        chatListEventBroadcaster.broadcastUpsertToUser(userId, event);
    }
}