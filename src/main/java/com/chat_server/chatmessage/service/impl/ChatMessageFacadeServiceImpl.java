package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.userblock.service.UserBlockService;
import com.chat_server.websocket.broadcaster.ChatMessageBroadCaster;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * chat message facade message service
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageBroadCaster chatMessageBroadCaster;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserBlockService userBlockService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatListService chatListService;

    @Override
    public void sendMessage(ChatSendRequest request, Long userId) {
        Long roomId = request.roomId();
        String messageType = request.messageType();
        String message = request.text();

        ChatRoom room = chatRoomQueryService.getRoomOrThrow(roomId);
        Long memberId = chatRoomQueryService.getMemberId(room.getId(), userId);

        validateSendPermission(room, userId, memberId, request);
        ChatMessage chatMessage = chatMessageService.createChatMessage(room, userId, messageType, message);

        updateRoomAndChatListOnSend(room, chatMessage);
        chatListService.increaseUnreadCount(roomId, userId);

        Long messageId = chatMessage.getId();
        String messageContent = chatMessage.getMessageContent();
        LocalDateTime createdAt = chatMessage.getCreatedAt();

        ChatMessageSenderResponse sender = new ChatMessageSenderResponse(
                userId,
                chatMessage.getSender().getUuid(),
                chatMessage.getSender().getNickname(),
                null,
                true
        );

        ChatMessageResponse response = new ChatMessageResponse(
                messageId,
                roomId,
                sender,
                messageContent,
                createdAt
        );

        chatMessageBroadCaster.broadcastMessage(response);
    }

    private void validateSendPermission(ChatRoom room, Long userId, Long memberId, ChatSendRequest request) {
        chatRoomQueryService.validateMemberOrThrow(room.getId(), userId);
        userBlockService.validateSenderNotBlocked(userId, memberId);
    }

    private void updateRoomAndChatListOnSend(ChatRoom room, ChatMessage chatMessage) {
        chatRoomService.updateLastMessageInfo(room, chatMessage);
    }
}
