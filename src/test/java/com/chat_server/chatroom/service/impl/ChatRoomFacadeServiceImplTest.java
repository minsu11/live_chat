//package com.chat_server.chatroom.service.impl;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
//import com.chat_server.chatlist.dto.response.ChatListItemResponse;
//import com.chat_server.chatlist.service.ChatListService;
//import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
//import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
//import com.chat_server.chatmessage.service.ChatMessageService;
//import com.chat_server.chatread.service.ChatReadFacadeService;
//import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
//import com.chat_server.chatroom.dto.response.ChatRoomResult;
//import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
//import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;
//import com.chat_server.chatroom.entity.ChatRoom;
//import com.chat_server.chatroom.enums.RoomType;
//import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
//import com.chat_server.chatroom.service.ChatRoomQueryService;
//import com.chat_server.chatroom.service.ChatRoomService;
//import com.chat_server.chatroommember.service.ChatRoomMemberService;
//import com.chat_server.common.cursor.ChatMessageCursorCodec;
//import com.chat_server.common.cursor.ChatMessageCursorKey;
//import com.chat_server.common.mapper.ChatListUpsertEventMapper;
//import com.chat_server.user.entity.User;
//import com.chat_server.user.service.UserDisplayNameService;
//import com.chat_server.user.service.UserService;
//import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Optional;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mockito;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Slice;
//import org.springframework.data.domain.SliceImpl;
//
//class ChatRoomFacadeServiceImplTest {
//
//    private ChatRoomService chatRoomService;
//    private ChatListService chatListService;
//    private ChatRoomMemberService chatRoomMemberService;
//    private ChatMessageService chatMessageService;
//    private ChatRoomQueryService chatRoomQueryService;
//    private UserService userService;
//    private UserDisplayNameService userDisplayNameService;
//    private ChatReadFacadeService chatReadFacadeService;
//    private ChatListEventBroadcaster chatListEventBroadcaster;
//    private ChatRoomDisplayResolver chatRoomDisplayResolver;
//    private ChatListUpsertEventMapper chatListUpsertEventMapper;
//
//    private ChatRoomFacadeServiceImpl target;
//
//    @BeforeEach
//    void setUp() {
//        chatRoomService = mock(ChatRoomService.class);
//        chatListService = mock(ChatListService.class);
//        chatRoomMemberService = mock(ChatRoomMemberService.class);
//        chatMessageService = mock(ChatMessageService.class);
//        chatRoomQueryService = mock(ChatRoomQueryService.class);
//        userService = mock(UserService.class);
//        userDisplayNameService = mock(UserDisplayNameService.class);
//        chatReadFacadeService = mock(ChatReadFacadeService.class);
//        chatListEventBroadcaster = mock(ChatListEventBroadcaster.class);
//        chatRoomDisplayResolver = mock(ChatRoomDisplayResolver.class);
//        chatListUpsertEventMapper = mock(ChatListUpsertEventMapper.class);
//
//        target = new ChatRoomFacadeServiceImpl(
//                chatRoomService,
//                chatListService,
//                chatRoomMemberService,
//                chatMessageService,
//                chatRoomQueryService,
//                userService,
//                userDisplayNameService,
//                chatReadFacadeService,
//                chatListEventBroadcaster,
//                chatRoomDisplayResolver,
//                chatListUpsertEventMapper
//        );
//    }
//
//    @Test
//    @DisplayName("hasNext=true면 nextCursor를 생성하고 메시지를 시간순으로 정렬한다")
//    void shouldReturnSortedMessagesAndNextCursorWhenHasNext() {
//        Long roomId = 10L;
//        Long userId = 99L;
//        int limit = 50;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.DM)
//                .name("테스트방")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        LocalDateTime t1 = LocalDateTime.of(2026, 3, 20, 10, 0, 0);
//        LocalDateTime t2 = LocalDateTime.of(2026, 3, 20, 10, 1, 0);
//
//        ChatMessageItemResponse newer = new ChatMessageItemResponse(
//                20L, 1L, "uuid-1", "u1", "profile1", "TEXT", "new", t2
//        );
//        ChatMessageItemResponse older = new ChatMessageItemResponse(
//                10L, 2L, "uuid-2", "u2", "profile2", "TEXT", "old", t1
//        );
//
//        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
//                List.of(newer, older),
//                PageRequest.of(0, limit),
//                true
//        );
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class)))
//                .thenReturn(slice);
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("상대 별칭");
//        when(userDisplayNameService.resolveDisplayName(eq(1L), eq(userId))).thenReturn(Optional.of("표시이름1"));
//        when(userDisplayNameService.resolveDisplayName(eq(2L), eq(userId))).thenReturn(Optional.of("표시이름2"));
//
//        long cursorMillis = t2.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
//        String cursor = ChatMessageCursorCodec.encode(cursorMillis, 20L);
//
//        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, cursor, limit);
//
//        verify(chatRoomQueryService).validateMemberOrThrow(roomId, userId);
//        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class));
//        verify(chatReadFacadeService, never()).markAsReadOnEnter(anyLong(), anyLong(), any());
//
//        assertThat(response.roomId()).isEqualTo(roomId);
//        assertThat(response.roomType()).isEqualTo("DM");
//        assertThat(response.title()).isEqualTo("상대 별칭");
//        assertThat(response.messages()).extracting(ChatMessageResponse::messageId)
//                .containsExactly(10L, 20L);
//
//        ChatMessageCursorKey next = ChatMessageCursorCodec.decode(response.nextCursor());
//        assertThat(next).isNotNull();
//        assertThat(next.lastMessageId()).isEqualTo(10L);
//    }
//
//    @Test
//    @DisplayName("첫 진입이면 markAsReadOnEnter를 호출한다")
//    void shouldMarkAsReadOnInitialEnter() {
//        Long roomId = 1L;
//        Long userId = 2L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("group")
//                .lastMessageId(999L)
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
//                List.of(),
//                PageRequest.of(0, 50),
//                false
//        );
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
//                .thenReturn(slice);
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("group");
//
//        target.enterChatRoom(roomId, userId, null, 50);
//
//        verify(chatReadFacadeService).markAsReadOnEnter(roomId, userId, 999L);
//    }
//
//    @Test
//    @DisplayName("hasNext=false면 nextCursor는 null이다")
//    void shouldReturnNullNextCursorWhenHasNextIsFalse() {
//        Long roomId = 2L;
//        Long userId = 3L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("group")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
//                List.of(
//                        new ChatMessageItemResponse(
//                                1L, 1L, "uuid", "u", "profileUrl", "TEXT", "hi", LocalDateTime.now()
//                        )
//                ),
//                PageRequest.of(0, 50),
//                false
//        );
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
//                .thenReturn(slice);
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("group");
//        when(userDisplayNameService.resolveDisplayName(anyLong(), eq(userId))).thenReturn(Optional.of("표시이름"));
//
//        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);
//
//        assertThat(response.nextCursor()).isNull();
//    }
//
//    @Test
//    @DisplayName("limit가 0 이하일 때는 1로 보정해서 조회한다")
//    void shouldClampLimitToOneWhenLimitIsNonPositive() {
//        Long roomId = 3L;
//        Long userId = 4L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("room")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(1), Mockito.isNull()))
//                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 1), false));
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("room");
//
//        target.enterChatRoom(roomId, userId, null, 0);
//
//        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(1), Mockito.isNull());
//    }
//
//    @Test
//    @DisplayName("limit가 100보다 크면 100으로 보정해서 조회한다")
//    void shouldClampLimitToHundredWhenLimitIsTooLarge() {
//        Long roomId = 4L;
//        Long userId = 5L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("room")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(100), Mockito.isNull()))
//                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 100), false));
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("room");
//
//        target.enterChatRoom(roomId, userId, null, 999);
//
//        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(100), Mockito.isNull());
//    }
//
//    @Test
//    @DisplayName("깨진 커서는 null 커서처럼 처리되어 첫 페이지를 조회한다")
//    void shouldTreatBrokenCursorAsFirstPage() {
//        Long roomId = 5L;
//        Long userId = 6L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("room")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
//                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("room");
//
//        target.enterChatRoom(roomId, userId, "broken-cursor", 50);
//
//        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull());
//    }
//
//    @Test
//    @DisplayName("동일 createdAt인 메시지는 messageId 오름차순으로 정렬된다")
//    void shouldSortByMessageIdWhenCreatedAtIsSame() {
//        Long roomId = 6L;
//        Long userId = 7L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("room")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        LocalDateTime same = LocalDateTime.of(2026, 3, 20, 10, 0, 0);
//        ChatMessageItemResponse id20 = new ChatMessageItemResponse(
//                20L, 1L, "uuid1", "u1", "profile1", "TEXT", "m2", same
//        );
//        ChatMessageItemResponse id10 = new ChatMessageItemResponse(
//                10L, 2L, "uuid2", "u2", "profile2", "TEXT", "m1", same
//        );
//
//        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
//                List.of(id20, id10),
//                PageRequest.of(0, 50),
//                false
//        );
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
//                .thenReturn(slice);
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("room");
//        when(userDisplayNameService.resolveDisplayName(anyLong(), eq(userId)))
//                .thenAnswer(invocation -> Optional.of("표시이름-" + invocation.getArgument(0)));
//
//        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);
//
//        assertThat(response.messages()).extracting(ChatMessageResponse::messageId)
//                .containsExactly(10L, 20L);
//    }
//
//    @Test
//    @DisplayName("입력 커서는 디코딩되어 서비스에 전달된다")
//    void shouldPassDecodedCursorToService() {
//        Long roomId = 7L;
//        Long userId = 8L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("room")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        LocalDateTime now = LocalDateTime.of(2026, 3, 20, 10, 10, 0);
//        long millis = now.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
//        String cursor = ChatMessageCursorCodec.encode(millis, 111L);
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), any(ChatMessageCursorKey.class)))
//                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));
//        when(chatRoomDisplayResolver.resolveTitle(roomId, userId, room)).thenReturn("room");
//
//        target.enterChatRoom(roomId, userId, cursor, 50);
//
//        ArgumentCaptor<ChatMessageCursorKey> captor = ArgumentCaptor.forClass(ChatMessageCursorKey.class);
//        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(50), captor.capture());
//        assertThat(captor.getValue().lastMessageId()).isEqualTo(111L);
//        assertThat(captor.getValue().lastMessageAtEpochMillis()).isEqualTo(millis);
//    }
//
//    @Test
//    @DisplayName("채팅방 요약 조회 시 resolver 결과를 반환한다")
//    void shouldReturnSummaryFromResolver() {
//        Long roomId = 8L;
//        Long userId = 9L;
//
//        ChatRoom room = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("그룹방")
//                .createdBy(User.builder().id(1L).build())
//                .build();
//
//        ChatRoomSummaryResponse summary = mock(ChatRoomSummaryResponse.class);
//
//        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
//        when(chatRoomDisplayResolver.resolveSummary(roomId, userId, room)).thenReturn(summary);
//
//        ChatRoomSummaryResponse result = target.getChatRoomSummary(roomId, userId);
//
//        verify(chatRoomQueryService).validateMemberOrThrow(roomId, userId);
//        verify(chatRoomDisplayResolver).resolveSummary(roomId, userId, room);
//        assertThat(result).isEqualTo(summary);
//    }
//
//    @Test
//    @DisplayName("1대1 채팅방 생성 시 두 사용자의 membership을 보장한다")
//    void shouldEnsureMembershipWhenCreatingOneToOneChatRoom() {
//        Long userId = 1L;
//        Long friendId = 2L;
//        String friendUuid = "friend-uuid";
//
//        ChatRoomResult result = new ChatRoomResult(100L, "DM");
//
//        when(userService.getUserIdByUserUuid(friendUuid)).thenReturn(friendId);
//        when(chatRoomService.getOrCreateOneToOneChatRoom(userId, friendId)).thenReturn(result);
//
//        ChatRoomResult response = target.getOrCreateOneToOneChatRoom(userId, friendUuid);
//
//        assertThat(response).isEqualTo(result);
//        verify(chatListService).ensureMembership(100L, userId);
//        verify(chatListService).ensureMembership(100L, friendId);
//        verify(chatRoomMemberService).ensureMembership(userId, 100L);
//        verify(chatRoomMemberService).ensureMembership(friendId, 100L);
//    }
//
//    @Test
//    @DisplayName("그룹 채팅방 생성 시 모든 참여자에게 chat list upsert 이벤트를 전송한다")
//    void shouldBroadcastChatListUpsertEventsWhenCreatingGroupChatRoom() {
//        Long requesterUserId = 1L;
//        Long roomId = 200L;
//
//        var request = new com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest(
//                "새 그룹방",
//                List.of("uuid-2", "uuid-3")
//        );
//
//        ChatRoom createdRoom = ChatRoom.builder()
//                .id(roomId)
//                .roomType(RoomType.GROUP)
//                .name("새 그룹방")
//                .createdBy(User.builder().id(requesterUserId).build())
//                .build();
//
//        when(userService.getUserIdByUserUuid("uuid-2")).thenReturn(2L);
//        when(userService.getUserIdByUserUuid("uuid-3")).thenReturn(3L);
//        when(chatRoomService.createGroupChatRoom("새 그룹방", requesterUserId)).thenReturn(createdRoom);
//
//        ChatListItemResponse item1 = mock(ChatListItemResponse.class);
//        ChatListItemResponse item2 = mock(ChatListItemResponse.class);
//        ChatListItemResponse item3 = mock(ChatListItemResponse.class);
//
//        ChatListUpsertEvent event1 = mock(ChatListUpsertEvent.class);
//        ChatListUpsertEvent event2 = mock(ChatListUpsertEvent.class);
//        ChatListUpsertEvent event3 = mock(ChatListUpsertEvent.class);
//
//        when(chatListService.getChatListItem(roomId, requesterUserId)).thenReturn(item1);
//        when(chatListService.getChatListItem(roomId, 2L)).thenReturn(item2);
//        when(chatListService.getChatListItem(roomId, 3L)).thenReturn(item3);
//
//        when(chatListUpsertEventMapper.toChatListUpsertEvent(item1)).thenReturn(event1);
//        when(chatListUpsertEventMapper.toChatListUpsertEvent(item2)).thenReturn(event2);
//        when(chatListUpsertEventMapper.toChatListUpsertEvent(item3)).thenReturn(event3);
//
//        CreateChatRoomResponse response = target.createGroupChatRoom(requesterUserId, request);
//
//        assertThat(response.roomId()).isEqualTo(roomId);
//        assertThat(response.roomType()).isEqualTo("GROUP");
//        assertThat(response.title()).isEqualTo("새 그룹방");
//
//        verify(chatRoomMemberService).ensureMembership(requesterUserId, roomId);
//        verify(chatRoomMemberService).ensureMembership(2L, roomId);
//        verify(chatRoomMemberService).ensureMembership(3L, roomId);
//
//        verify(chatListService).ensureMembership(roomId, requesterUserId);
//        verify(chatListService).ensureMembership(roomId, 2L);
//        verify(chatListService).ensureMembership(roomId, 3L);
//
//        verify(chatListEventBroadcaster).broadcastUpsertToUser(requesterUserId, event1);
//        verify(chatListEventBroadcaster).broadcastUpsertToUser(2L, event2);
//        verify(chatListEventBroadcaster).broadcastUpsertToUser(3L, event3);
//    }
//}