package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatMessageFacadeServiceImplFailureTest {

    private ChatMessageBroadCaster chatMessageBroadCaster;
    private ChatRoomQueryService chatRoomQueryService;
    private UserBlockService userBlockService;
    private ChatMessageService chatMessageService;
    private ChatListService chatListService;
    private UserProfileImageService userProfileImageService;
    private ChatNotificationBroadcaster chatNotificationBroadcaster;
    private ChatListEventBroadcaster chatListEventBroadcaster;
    private ChatAttachmentService chatAttachmentService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatMetadataRedisService chatMetadataRedisService;
    private ChatMessageFacadeServiceImpl target;

    @BeforeEach
    void setUp() {
        chatMessageBroadCaster = mock(ChatMessageBroadCaster.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        userBlockService = mock(UserBlockService.class);
        chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        chatListService = mock(ChatListService.class);
        UserDisplayNameService userDisplayNameService = mock(UserDisplayNameService.class);
        userProfileImageService = mock(UserProfileImageService.class);
        chatNotificationBroadcaster = mock(ChatNotificationBroadcaster.class);
        ChatNotificationEventMapper chatNotificationEventMapper = mock(ChatNotificationEventMapper.class);
        ChatRoomDisplayResolver chatRoomDisplayResolver = mock(ChatRoomDisplayResolver.class);
        chatListEventBroadcaster = mock(ChatListEventBroadcaster.class);
        ChatListUpsertEventMapper chatListUpsertEventMapper = mock(ChatListUpsertEventMapper.class);
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

    /**
     * 요청 객체가 null인 validation 실패 경로를 검증한다.
     * 요청 값을 읽는 단계에서 예외가 발생하며 어떤 저장소나 외부 전파 기능도 호출되면 안 된다.
     */
    @Test
    @DisplayName("메시지 전송 validation 실패 - 요청 객체가 null이면 즉시 예외가 발생하고 외부 기능을 호출하지 않는다")
    void sendMessageShouldFailImmediatelyWhenRequestIsNull() {
        assertThatThrownBy(() -> target.sendMessage(null, 1L))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(chatRoomQueryService, chatMessageService, chatMetadataRedisService,
                chatMessageBroadCaster, chatListEventBroadcaster, chatNotificationBroadcaster);
    }

    /**
     * 존재하지 않는 채팅방을 조회했을 때 발생한 예외가 그대로 전파되는지 검증한다.
     * 방 조회에 실패한 뒤 프로필 조회, 권한 검증, 메시지 저장이 이어지면 안 된다.
     */
    @Test
    @DisplayName("메시지 전송 실패 - 채팅방이 존재하지 않으면 예외를 전파하고 메시지를 저장하지 않는다")
    void sendMessageShouldStopWhenRoomLookupFails() {
        RuntimeException roomNotFound = new RuntimeException("chat room not found");
        when(chatRoomQueryService.getRoomOrThrow(10L)).thenThrow(roomNotFound);

        assertThatThrownBy(() -> target.sendMessage(
                new ChatSendRequest(10L, MessageType.TEXT, "안녕", "client-1"), 1L))
                .isSameAs(roomNotFound);

        verifyNoInteractions(userProfileImageService, chatMessageService, chatMetadataRedisService,
                chatMessageBroadCaster, chatListEventBroadcaster, chatNotificationBroadcaster);
    }

    /**
     * 도메인 메시지 저장 과정에서 DB 예외가 발생한 경로를 검증한다.
     * 저장되지 않은 메시지의 읽음 처리, Redis 메타데이터 갱신, 브로드캐스트가 실행되면 안 된다.
     */
    @Test
    @DisplayName("메시지 전송 실패 - 메시지 저장 중 예외가 발생하면 Redis 갱신과 브로드캐스트를 중단한다")
    void sendMessageShouldStopWhenMessagePersistenceFails() {
        ChatRoom room = chatRoom(20L);
        RuntimeException persistenceFailure = new RuntimeException("database write failed");

        when(chatRoomQueryService.getRoomOrThrow(20L)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(1L)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, 1L, "TEXT", "안녕", "client-2"))
                .thenThrow(persistenceFailure);

        assertThatThrownBy(() -> target.sendMessage(
                new ChatSendRequest(20L, MessageType.TEXT, "안녕", "client-2"), 1L))
                .isSameAs(persistenceFailure);

        verify(chatRoomQueryService).validateMemberOrThrow(20L, 1L);
        verifyNoInteractions(chatAttachmentService, chatRoomMemberService,
                chatMessageBroadCaster, chatListEventBroadcaster, chatNotificationBroadcaster);
        verify(chatMetadataRedisService, never()).markAsRead(anyLong(), anyLong(), anyLong(), any());
        verify(chatMetadataRedisService, never()).updateRoomMeta(anyLong(), anyLong(), anyString(), any());
    }

    /**
     * 메시지 저장 후 발신자 읽음 상태를 Redis에 반영하는 과정에서 장애가 발생한 경로를 검증한다.
     * 메타데이터 정합성이 확보되지 않았으므로 첨부 연결과 브로드캐스트를 진행하지 않아야 한다.
     */
    @Test
    @DisplayName("메시지 전송 실패 - Redis 읽음 상태 갱신이 실패하면 이후 메타 갱신과 브로드캐스트를 중단한다")
    void sendMessageShouldStopWhenMarkAsReadFails() {
        ChatRoom room = chatRoom(30L);
        ChatMessage message = chatMessage(room, 1L, 300L, MessageType.TEXT, "안녕");
        RuntimeException redisFailure = new RuntimeException("redis unavailable");

        when(chatRoomQueryService.getRoomOrThrow(30L)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(1L)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, 1L, "TEXT", "안녕", null)).thenReturn(message);
        doThrow(redisFailure).when(chatMetadataRedisService)
                .markAsRead(eq(30L), eq(1L), eq(300L), any(LocalDateTime.class));

        assertThatThrownBy(() -> target.sendMessage(
                new ChatSendRequest(30L, MessageType.TEXT, "안녕", null), 1L))
                .isSameAs(redisFailure);

        verify(chatMetadataRedisService, never()).updateRoomMeta(anyLong(), anyLong(), anyString(), any());
        verifyNoInteractions(chatAttachmentService, chatRoomMemberService,
                chatMessageBroadCaster, chatListEventBroadcaster, chatNotificationBroadcaster);
    }

    /**
     * FILE 메시지 JSON은 정상 형식이지만 attachmentId가 null인 validation 실패를 검증한다.
     * 첨부파일 연결, 방 메타데이터 갱신, 실시간 전파가 수행되지 않아야 한다.
     */
    @Test
    @DisplayName("파일 메시지 validation 실패 - attachmentId가 null이면 첨부 연결과 브로드캐스트를 하지 않는다")
    void sendMessageShouldRejectFilePayloadWithoutAttachmentId() {
        ChatRoom room = chatRoom(40L);
        String payload = "{\"attachmentId\":null,\"fileName\":\"a.pdf\",\"contentType\":\"application/pdf\",\"fileSize\":10}";
        ChatMessage message = chatMessage(room, 1L, 400L, MessageType.FILE, payload);

        when(chatRoomQueryService.getRoomOrThrow(40L)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(1L)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, 1L, "FILE", payload, null)).thenReturn(message);

        assertThatThrownBy(() -> target.sendMessage(
                new ChatSendRequest(40L, MessageType.FILE, payload, null), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FILE 메시지 첨부 정보가 올바르지 않습니다.")
                .hasRootCauseMessage("attachmentId가 비어 있습니다.");

        verify(chatAttachmentService, never()).connectMessage(anyLong(), any(), anyLong());
        verify(chatMetadataRedisService, never()).updateRoomMeta(anyLong(), anyLong(), anyString(), any());
        verifyNoInteractions(chatRoomMemberService, chatMessageBroadCaster,
                chatListEventBroadcaster, chatNotificationBroadcaster);
    }

    /**
     * 방 메타데이터 갱신까지 성공했지만 참여자 읽음 상태 조회가 실패하는 예외 경로를 검증한다.
     * 수신자별 unread 증가, 메시지/목록/알림 브로드캐스트가 시작되지 않아야 한다.
     */
    @Test
    @DisplayName("메시지 전송 실패 - 참여자 읽음 상태 조회 중 Redis 예외가 발생하면 수신자 전파를 중단한다")
    void sendMessageShouldStopBeforeReceiverBroadcastWhenReadMapLookupFails() {
        ChatRoom room = chatRoom(50L);
        ChatMessage message = chatMessage(room, 1L, 500L, MessageType.TEXT, "안녕");
        RuntimeException redisFailure = new RuntimeException("redis hash read failed");

        when(chatRoomQueryService.getRoomOrThrow(50L)).thenReturn(room);
        when(userProfileImageService.getUserProfileUrl(1L)).thenReturn("profile-url");
        when(chatMessageService.createChatMessage(room, 1L, "TEXT", "안녕", null)).thenReturn(message);
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(50L)).thenReturn(java.util.List.of(1L, 2L));
        when(chatMetadataRedisService.getAllMembersLastReadId(50L, java.util.List.of(1L, 2L)))
                .thenThrow(redisFailure);

        assertThatThrownBy(() -> target.sendMessage(
                new ChatSendRequest(50L, MessageType.TEXT, "안녕", null), 1L))
                .isSameAs(redisFailure);

        verify(chatMetadataRedisService).updateRoomMeta(50L, 500L, "안녕", message.getCreatedAt());
        verify(chatMetadataRedisService, never()).incrementUnreadCount(anyLong(), anyLong());
        verifyNoInteractions(chatMessageBroadCaster, chatListEventBroadcaster, chatNotificationBroadcaster);
    }

    private ChatRoom chatRoom(Long roomId) {
        return ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(user(99L, "creator", "방장"))
                .build();
    }

    private ChatMessage chatMessage(
            ChatRoom room,
            Long senderId,
            Long messageId,
            MessageType messageType,
            String content
    ) {
        ChatMessage chatMessage = ChatMessage.create(
                room,
                user(senderId, "sender-uuid", "보낸사람"),
                content,
                messageType.name(),
                null
        );
        ReflectionTestUtils.setField(chatMessage, "id", messageId);
        ReflectionTestUtils.setField(chatMessage, "createdAt", LocalDateTime.of(2026, 7, 20, 12, 0));
        return chatMessage;
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
}
