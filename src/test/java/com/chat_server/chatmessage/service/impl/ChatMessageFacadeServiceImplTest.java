package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatMessageResponseMapper;
import com.chat_server.common.mapper.ChatNotificationEventMapper;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.userblock.service.UserBlockService;
import com.chat_server.userprofileImage.service.UserProfileImageService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageBroadCaster;
import com.chat_server.websocket.broadcaster.chatmessage.ChatNotificationBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatMessageFacadeServiceImplTest {

    private ChatMessageBroadCaster chatMessageBroadCaster;
    private ChatRoomQueryService chatRoomQueryService;
    private UserBlockService userBlockService;
    private ChatMessageService chatMessageService;
    private ChatRoomService chatRoomService;
    private ChatListService chatListService;
    private UserDisplayNameService userDisplayNameService;
    private UserProfileImageService userProfileImageService;
    private ChatNotificationBroadcaster chatNotificationBroadcaster;
    private ChatNotificationEventMapper chatNotificationEventMapper;
    private ChatRoomDisplayResolver chatRoomDisplayResolver;
    private ChatListEventBroadcaster chatListEventBroadcaster;
    private ChatListUpsertEventMapper chatListUpsertEventMapper;
    private ChatAttachmentService chatAttachmentService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatMetadataRedisService chatMetadataRedisService;

    private ChatMessageFacadeService target;

    @BeforeEach
    void setUp() {
        chatMessageBroadCaster = mock(ChatMessageBroadCaster.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        userBlockService = mock(UserBlockService.class);
        chatMessageService = mock(ChatMessageService.class);
        chatRoomService = mock(ChatRoomService.class);
        chatListService = mock(ChatListService.class);
        userDisplayNameService = mock(UserDisplayNameService.class);
        userProfileImageService = mock(UserProfileImageService.class);
        chatNotificationBroadcaster = mock(ChatNotificationBroadcaster.class);
        chatNotificationEventMapper = mock(ChatNotificationEventMapper.class);
        chatRoomDisplayResolver = mock(ChatRoomDisplayResolver.class);
        chatListEventBroadcaster = mock(ChatListEventBroadcaster.class);
        chatListUpsertEventMapper = mock(ChatListUpsertEventMapper.class);
        chatAttachmentService = mock(ChatAttachmentService.class);
        chatRoomMemberService = mock(ChatRoomMemberService.class);
        chatMetadataRedisService = mock(ChatMetadataRedisService.class);

        target = new ChatMessageFacadeServiceImpl(
                new ChatMessageResponseMapper(),
                chatMessageBroadCaster,
                chatRoomQueryService,
                userBlockService,
                chatMessageService,
                chatRoomService,
                chatListService,
                userDisplayNameService,
                userProfileImageService,
                chatNotificationBroadcaster,
                chatNotificationEventMapper,
                chatRoomDisplayResolver,
                chatListEventBroadcaster,
                chatListUpsertEventMapper,
                new ObjectMapper(),
                chatAttachmentService,
                chatRoomMemberService,
                chatMetadataRedisService
        );
    }

    @Test
    @DisplayName("텍스트 메시지 전송 성공 시 저장, Redis 메타 갱신, 수신자별 브로드캐스트와 알림을 수행한다")
    void sendMessageShouldSaveUpdateRedisAndBroadcastPerReceiver() {
        Long roomId = 10L;
        Long senderId = 1L;
        ChatRoom room = chatRoom(roomId);
        ChatMessage chatMessage = chatMessage(room, senderId, "sender-uuid", "보낸사람", "TEXT", "안녕", "client-1", 100L);
        ChatSendRequest request = new ChatSendRequest(roomId, MessageType.TEXT, "안녕", "  client-1  ");
        ChatListItemResponse senderListItem = new ChatListItemResponse(roomId, "내 목록", 0, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatListItemResponse receiverListItem = new ChatListItemResponse(roomId, "친구 목록", 1, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatListUpsertEvent senderUpsert = new ChatListUpsertEvent(roomId, "UPSERT", "내 목록", 0, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatListUpsertEvent receiverUpsert = new ChatListUpsertEvent(roomId, "UPSERT", "친구 목록", 1, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatNotificationEvent notificationEvent = new ChatNotificationEvent(roomId, "NEW_MESSAGE", "친구방", "안녕", chatMessage.getCreatedAt());

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(senderId)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, senderId, "TEXT", "안녕", "client-1")).thenReturn(chatMessage);
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(roomId)).thenReturn(List.of(senderId, 2L));
        when(chatMetadataRedisService.getAllMembersLastReadId(roomId, List.of(senderId, 2L)))
                .thenReturn(Map.of(senderId, 100L, 2L, 50L));
        when(userBlockService.isBlocked(senderId, 2L)).thenReturn(false);
        when(userDisplayNameService.resolveDisplayName(senderId, senderId)).thenReturn(Optional.of("나"));
        when(userDisplayNameService.resolveDisplayName(senderId, 2L)).thenReturn(Optional.of("친구가 보는 이름"));
        when(chatListService.getChatListItem(roomId, senderId)).thenReturn(senderListItem);
        when(chatListService.getChatListItem(roomId, 2L)).thenReturn(receiverListItem);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(senderListItem)).thenReturn(senderUpsert);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(receiverListItem)).thenReturn(receiverUpsert);
        when(chatRoomDisplayResolver.resolveTitle(roomId, 2L, room)).thenReturn("친구방");
        when(chatNotificationEventMapper.toChatNotificationEvent(roomId, "친구방", "안녕", chatMessage.getCreatedAt()))
                .thenReturn(notificationEvent);

        target.sendMessage(request, senderId);

        verify(chatRoomQueryService).validateMemberOrThrow(roomId, senderId);
        verify(chatMetadataRedisService).markAsRead(eq(roomId), eq(senderId), eq(100L), any(LocalDateTime.class));
        verify(chatMetadataRedisService).updateRoomMeta(roomId, 100L, "안녕", chatMessage.getCreatedAt());
        verify(chatMetadataRedisService).incrementUnreadCount(roomId, 2L);
        verify(chatMetadataRedisService, never()).incrementUnreadCount(roomId, senderId);
        verify(chatAttachmentService, never()).connectMessage(any(), any(), any());

        ArgumentCaptor<ChatMessageResponse> responseCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(chatMessageBroadCaster).broadcastMessage(eq(senderId), responseCaptor.capture());
        ChatMessageResponse senderResponse = responseCaptor.getValue();
        assertThat(senderResponse.clientMessageId()).isEqualTo("client-1");
        assertThat(senderResponse.mine()).isTrue();
        assertThat(senderResponse.sender().senderNickname()).isEqualTo("나");
        assertThat(senderResponse.unreadCount()).isEqualTo(1);

        verify(chatMessageBroadCaster).broadcastMessage(eq(2L), responseCaptor.capture());
        ChatMessageResponse receiverResponse = responseCaptor.getValue();
        assertThat(receiverResponse.mine()).isFalse();
        assertThat(receiverResponse.sender().senderNickname()).isEqualTo("친구가 보는 이름");
        assertThat(receiverResponse.sender().profileImageUrl()).isEqualTo("profile-url");
        assertThat(receiverResponse.unreadCount()).isEqualTo(1);

        verify(chatListEventBroadcaster).broadcastUpsertToUser(senderId, senderUpsert);
        verify(chatListEventBroadcaster).broadcastUpsertToUser(2L, receiverUpsert);
        verify(chatNotificationBroadcaster).broadcastToUser(2L, notificationEvent);
        verify(chatNotificationBroadcaster, never()).broadcastToUser(eq(senderId), any());
    }

    @Test
    @DisplayName("파일 메시지 전송 성공 시 첨부 JSON을 해석해 메시지와 첨부를 연결한다")
    void sendMessageShouldConnectAttachmentWhenFilePayloadIsValid() {
        Long roomId = 20L;
        Long senderId = 1L;
        ChatRoom room = chatRoom(roomId);
        String payload = "{\"attachmentId\":77,\"fileName\":\"a.pdf\",\"contentType\":\"application/pdf\",\"fileSize\":10}";
        ChatMessage chatMessage = chatMessage(room, senderId, "sender-uuid", "보낸사람", "FILE", payload, null, 200L);
        ChatSendRequest request = new ChatSendRequest(roomId, MessageType.FILE, payload, null);
        ChatListItemResponse item = new ChatListItemResponse(roomId, "목록", 0, "파일", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatListUpsertEvent upsert = new ChatListUpsertEvent(roomId, "UPSERT", "목록", 0, "파일", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(senderId)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, senderId, "FILE", payload, null)).thenReturn(chatMessage);
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(roomId)).thenReturn(List.of(senderId));
        when(chatMetadataRedisService.getAllMembersLastReadId(roomId, List.of(senderId))).thenReturn(Map.of(senderId, 200L));
        when(userDisplayNameService.resolveDisplayName(senderId, senderId)).thenReturn(Optional.empty());
        when(chatListService.getChatListItem(roomId, senderId)).thenReturn(item);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(item)).thenReturn(upsert);

        target.sendMessage(request, senderId);

        verify(chatAttachmentService).connectMessage(77L, chatMessage, senderId);
        verify(chatMetadataRedisService).updateRoomMeta(roomId, 200L, "파일 보냈습니다.", chatMessage.getCreatedAt());
        verify(chatMessageBroadCaster).broadcastMessage(eq(senderId), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("메시지 전송 실패 시 멤버 권한 검증 예외를 전파하고 메시지를 저장하지 않는다")
    void sendMessageShouldStopWhenPermissionValidationFails() {
        Long roomId = 30L;
        Long senderId = 1L;
        ChatRoom room = chatRoom(roomId);
        RuntimeException forbidden = new RuntimeException("not a member");
        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(senderId)).thenReturn("profile-url");
        doThrow(forbidden).when(chatRoomQueryService).validateMemberOrThrow(roomId, senderId);

        assertThatThrownBy(() -> target.sendMessage(new ChatSendRequest(roomId, MessageType.TEXT, "안녕", null), senderId))
                .isSameAs(forbidden);

        verify(chatMessageService, never()).createChatMessage(any(), any(), any(), any(), any());
        verify(chatMetadataRedisService, never()).markAsRead(any(), any(), any(), any());
        verify(chatMessageBroadCaster, never()).broadcastMessage(any(), any());
    }

    @Test
    @DisplayName("파일 메시지 전송 실패 시 첨부 payload가 잘못되면 메타 갱신과 브로드캐스트를 하지 않는다")
    void sendMessageShouldThrowAndStopBeforeBroadcastWhenFilePayloadIsInvalid() {
        Long roomId = 40L;
        Long senderId = 1L;
        ChatRoom room = chatRoom(roomId);
        ChatMessage chatMessage = chatMessage(room, senderId, "sender-uuid", "보낸사람", "FILE", "not-json", null, 300L);
        ChatSendRequest request = new ChatSendRequest(roomId, MessageType.FILE, "not-json", null);

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(senderId)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, senderId, "FILE", "not-json", null)).thenReturn(chatMessage);

        assertThatThrownBy(() -> target.sendMessage(request, senderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FILE 메시지 첨부 정보가 올바르지 않습니다.");

        verify(chatAttachmentService, never()).connectMessage(any(), any(), any());
        verify(chatMetadataRedisService, never()).updateRoomMeta(any(), any(), any(), any());
        verify(chatMessageBroadCaster, never()).broadcastMessage(any(), any());
    }

    @Test
    @DisplayName("메시지 전송 성공 시 차단된 수신자는 브로드캐스트와 알림 대상에서 제외한다")
    void sendMessageShouldSkipBlockedReceiver() {
        Long roomId = 50L;
        Long senderId = 1L;
        Long blockedReceiverId = 2L;
        ChatRoom room = chatRoom(roomId);
        ChatMessage chatMessage = chatMessage(room, senderId, "sender-uuid", "보낸사람", "TEXT", "안녕", null, 400L);
        ChatListItemResponse senderListItem = new ChatListItemResponse(roomId, "내 목록", 0, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());
        ChatListUpsertEvent senderUpsert = new ChatListUpsertEvent(roomId, "UPSERT", "내 목록", 0, "안녕", chatMessage.getCreatedAt(), chatMessage.getCreatedAt());

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(senderId)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, senderId, "TEXT", "안녕", null)).thenReturn(chatMessage);
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(roomId)).thenReturn(List.of(senderId, blockedReceiverId));
        when(chatMetadataRedisService.getAllMembersLastReadId(roomId, List.of(senderId, blockedReceiverId)))
                .thenReturn(Map.of(senderId, 400L));
        when(userBlockService.isBlocked(senderId, blockedReceiverId)).thenReturn(true);
        when(userDisplayNameService.resolveDisplayName(senderId, senderId)).thenReturn(Optional.of("나"));
        when(chatListService.getChatListItem(roomId, senderId)).thenReturn(senderListItem);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(senderListItem)).thenReturn(senderUpsert);

        target.sendMessage(new ChatSendRequest(roomId, MessageType.TEXT, "안녕", null), senderId);

        verify(chatMessageBroadCaster).broadcastMessage(eq(senderId), any(ChatMessageResponse.class));
        verify(chatMessageBroadCaster, never()).broadcastMessage(eq(blockedReceiverId), any(ChatMessageResponse.class));
        verify(chatMetadataRedisService, never()).incrementUnreadCount(roomId, blockedReceiverId);
        verify(chatNotificationBroadcaster, never()).broadcastToUser(eq(blockedReceiverId), any());
    }

    private ChatRoom chatRoom(Long roomId) {
        return ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(user(99L, "creator", "방장"))
                .build();
    }

    private User user(Long id, String uuid, String nickname) {
        return User.builder()
                .id(id)
                .uuid(uuid)
                .nickname(nickname)
                .name(nickname)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }

    private ChatMessage chatMessage(
            ChatRoom room,
            Long senderId,
            String senderUuid,
            String senderNickname,
            String messageType,
            String content,
            String clientMessageId,
            Long messageId
    ) {
        ChatMessage chatMessage = ChatMessage.create(
                room,
                user(senderId, senderUuid, senderNickname),
                content,
                messageType,
                clientMessageId
        );
        ReflectionTestUtils.setField(chatMessage, "id", messageId);
        ReflectionTestUtils.setField(chatMessage, "createdAt", LocalDateTime.of(2026, 6, 11, 12, 0).plusMinutes(messageId));
        return chatMessage;
    }
}
