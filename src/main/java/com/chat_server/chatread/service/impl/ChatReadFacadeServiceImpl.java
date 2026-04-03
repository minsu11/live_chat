package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.user.service.UserService;
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

    @Override
    public void read(ChatReadRequest request, Long userId) {
        Long roomId = request.roomId();
        Long messageId = request.messageId();

        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        chatReadService.markAsRead(roomId, userId, messageId);

        String readerUserUuid = userService.getUuidByUserId(userId);
        List<UpdatedMessageUnreadCount> updatedMessageUnreadCounts =
                chatMessageService.findUpdatedUnreadCounts(roomId, messageId);
        ChatReadUpdatedEvent event = new ChatReadUpdatedEvent(
                roomId,
                readerUserUuid,
                messageId,
                updatedMessageUnreadCounts
        );

        chatMessageReadBroadcaster.readRoomBroadcast(roomId, event);
    }

    @Override
    public void markAsReadOnEnter(Long roomId, Long userId, Long latestMessageId) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        chatReadService.markAsReadOnEnter(roomId, userId, latestMessageId);

        if (latestMessageId == null) {
            return;
        }

        String readerUserUuid = userService.getUuidByUserId(userId);

        List<UpdatedMessageUnreadCount> updatedMessageUnreadCounts =
                chatMessageService.findUpdatedUnreadCounts(roomId, latestMessageId);
        ChatReadUpdatedEvent event = new ChatReadUpdatedEvent(
                roomId,
                readerUserUuid,
                latestMessageId,
                updatedMessageUnreadCounts
        );

        chatMessageReadBroadcaster.readRoomBroadcast(roomId, event);
    }
}