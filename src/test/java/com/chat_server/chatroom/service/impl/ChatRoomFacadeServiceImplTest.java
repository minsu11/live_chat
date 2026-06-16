package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageCatchUpResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatMessageResponseMapper;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatRoomFacadeServiceImplTest {

    private ChatRoomService chatRoomService;
    private ChatListService chatListService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatMessageService chatMessageService;
    private ChatRoomQueryService chatRoomQueryService;
    private UserService userService;
    private UserDisplayNameService userDisplayNameService;
    private ChatReadFacadeService chatReadFacadeService;
    private ChatListEventBroadcaster chatListEventBroadcaster;
    private ChatRoomDisplayResolver chatRoomDisplayResolver;
    private ChatListUpsertEventMapper chatListUpsertEventMapper;
    private ChatMessageFacadeService chatMessageFacadeService;
    private ChatMetadataRedisService chatMetadataRedisService;

    private ChatRoomFacadeServiceImpl target;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatListService = mock(ChatListService.class);
        chatRoomMemberService = mock(ChatRoomMemberService.class);
        chatMessageService = mock(ChatMessageService.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        userService = mock(UserService.class);
        userDisplayNameService = mock(UserDisplayNameService.class);
        chatReadFacadeService = mock(ChatReadFacadeService.class);
        chatListEventBroadcaster = mock(ChatListEventBroadcaster.class);
        chatRoomDisplayResolver = mock(ChatRoomDisplayResolver.class);
        chatListUpsertEventMapper = mock(ChatListUpsertEventMapper.class);
        chatMessageFacadeService = mock(ChatMessageFacadeService.class);
        chatMetadataRedisService = mock(ChatMetadataRedisService.class);

        target = new ChatRoomFacadeServiceImpl(
                chatRoomService,
                chatListService,
                chatRoomMemberService,
                chatMessageService,
                chatRoomQueryService,
                userService,
                userDisplayNameService,
                chatReadFacadeService,
                chatListEventBroadcaster,
                chatRoomDisplayResolver,
                chatListUpsertEventMapper,
                chatMessageFacadeService,
                chatMetadataRedisService,
                new ObjectMapper(),
                new ChatMessageResponseMapper()
        );
    }

    @Test
    @DisplayName("채팅방 최초 진입 성공 시 읽음 처리, 표시 이름, 안읽음 수, 정렬, 다음 커서를 반환한다")
    void enterChatRoomShouldMarkReadAndReturnSortedMessagesWithUnreadCountsAndNextCursor() {
        Long roomId = 10L;
        Long viewerId = 99L;
        ChatRoom room = chatRoom(roomId, RoomType.GROUP, "그룹방", 30L);
        LocalDateTime olderTime = LocalDateTime.of(2026, 6, 11, 9, 0);
        LocalDateTime newerTime = LocalDateTime.of(2026, 6, 11, 9, 1);
        ChatMessageItemResponse newer = messageItem(20L, "client-20", 2L, "uuid-2", "기본이름2", "TEXT", "new", newerTime, 0);
        ChatMessageItemResponse older = messageItem(10L, "client-10", 1L, "uuid-1", "기본이름1", "TEXT", "old", olderTime, 0);
        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(List.of(newer, older), PageRequest.of(0, 50), true);

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMetadataRedisService.getLatestMessageId(roomId, 30L)).thenReturn(30L);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(viewerId), eq(100), isNull()))
                .thenReturn(slice);
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(roomId))
                .thenReturn(List.of(1L, 2L, viewerId));
        when(chatMetadataRedisService.getAllMembersLastReadId(eq(roomId), anyList()))
                .thenReturn(new java.util.HashMap<>(Map.of(1L, 10L, 2L, 5L)));
        when(userDisplayNameService.resolveDisplayNamesBulk(eq(viewerId), anyList()))
                .thenReturn(Map.of(1L, "친구별칭1"));
        when(chatRoomDisplayResolver.resolveTitle(roomId, viewerId, room)).thenReturn("화면 제목");
        when(chatListService.getMuted(roomId, viewerId)).thenReturn(true);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, viewerId, null, 999);

        verify(chatRoomQueryService).validateMemberOrThrow(roomId, viewerId);
        verify(chatReadFacadeService).markAsReadOnEnter(roomId, viewerId, 30L);
        verify(chatMessageService).getEnterMessagesByCursor(roomId, viewerId, 100, null);
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.roomType()).isEqualTo("GROUP");
        assertThat(response.title()).isEqualTo("화면 제목");
        assertThat(response.muted()).isTrue();
        assertThat(response.messages()).extracting(ChatMessageResponse::messageId).containsExactly(10L, 20L);
        assertThat(response.messages().get(0).sender().senderNickname()).isEqualTo("친구별칭1");
        assertThat(response.messages().get(0).unreadCount()).isEqualTo(1);
        assertThat(response.messages().get(1).sender().senderNickname()).isEqualTo("기본이름2");
        assertThat(response.messages().get(1).unreadCount()).isEqualTo(1);

        ChatMessageCursorKey nextCursor = ChatMessageCursorCodec.decode(response.nextCursor());
        assertThat(nextCursor).isNotNull();
        assertThat(nextCursor.lastMessageId()).isEqualTo(10L);
        assertThat(nextCursor.lastMessageAtEpochMillis()).isEqualTo(olderTime.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("채팅방 추가 조회 성공 시 커서를 디코딩하고 최초 진입 읽음 처리는 하지 않는다")
    void enterChatRoomShouldDecodeCursorAndSkipReadMarkWhenCursorExists() {
        Long roomId = 20L;
        Long viewerId = 3L;
        ChatRoom room = chatRoom(roomId, RoomType.DM, "dm", 111L);
        LocalDateTime cursorTime = LocalDateTime.of(2026, 6, 11, 10, 30);
        String cursor = ChatMessageCursorCodec.encode(cursorTime.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli(), 123L);

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMetadataRedisService.getLatestMessageId(roomId, 111L)).thenReturn(111L);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(viewerId), eq(1), any(ChatMessageCursorKey.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 1), false));
        when(chatRoomMemberService.getRoomMemberIdsByRoomId(roomId)).thenReturn(List.of(3L, 4L));
        when(chatMetadataRedisService.getAllMembersLastReadId(roomId, List.of(3L, 4L))).thenReturn(Map.of());
        when(userDisplayNameService.resolveDisplayNamesBulk(viewerId, List.of())).thenReturn(Map.of());
        when(chatRoomDisplayResolver.resolveTitle(roomId, viewerId, room)).thenReturn("dm title");

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, viewerId, cursor, 0);

        ArgumentCaptor<ChatMessageCursorKey> cursorCaptor = ArgumentCaptor.forClass(ChatMessageCursorKey.class);
        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(viewerId), eq(1), cursorCaptor.capture());
        verify(chatReadFacadeService, never()).markAsReadOnEnter(any(), any(), any());
        assertThat(cursorCaptor.getValue().lastMessageId()).isEqualTo(123L);
        assertThat(response.nextCursor()).isNull();
        assertThat(response.messages()).isEmpty();
    }

    @Test
    @DisplayName("채팅방 진입 실패 시 멤버 검증 예외를 전파하고 메시지를 조회하지 않는다")
    void enterChatRoomShouldPropagateMembershipFailureAndStopFlow() {
        Long roomId = 30L;
        Long viewerId = 7L;
        RuntimeException forbidden = new RuntimeException("not a member");
        doThrow(forbidden).when(chatRoomQueryService).validateMemberOrThrow(roomId, viewerId);

        assertThatThrownBy(() -> target.enterChatRoom(roomId, viewerId, null, 50))
                .isSameAs(forbidden);

        verify(chatRoomQueryService, never()).getRoomOrThrow(any());
        verify(chatMessageService, never()).getEnterMessagesByCursor(any(), any(), anyInt(), any());
        verify(chatReadFacadeService, never()).markAsReadOnEnter(any(), any(), any());
    }

    @Test
    @DisplayName("1대1 채팅방 생성 성공 시 양쪽 사용자 채팅방 멤버십과 채팅 목록을 보장한다")
    void getOrCreateOneToOneChatRoomShouldEnsureMembershipsForBothUsers() {
        Long userId = 1L;
        Long friendId = 2L;
        String friendUuid = "friend-uuid";
        ChatRoomResult result = new ChatRoomResult(100L, true);

        when(userService.getUserIdByUserUuid(friendUuid)).thenReturn(friendId);
        when(chatRoomService.getOrCreateOneToOneChatRoom(userId, friendId)).thenReturn(result);

        ChatRoomResult response = target.getOrCreateOneToOneChatRoom(userId, friendUuid);

        assertThat(response).isEqualTo(result);
        verify(chatListService).ensureMembership(100L, userId);
        verify(chatListService).ensureMembership(100L, friendId);
        verify(chatRoomMemberService).ensureMembership(userId, 100L);
        verify(chatRoomMemberService).ensureMembership(friendId, 100L);
    }

    @Test
    @DisplayName("1대1 채팅방 생성 실패 시 자기 자신과의 방 생성을 거부한다")
    void getOrCreateOneToOneChatRoomShouldRejectSelfChat() {
        Long userId = 1L;
        String myUuid = "my-uuid";
        when(userService.getUserIdByUserUuid(myUuid)).thenReturn(userId);

        assertThatThrownBy(() -> target.getOrCreateOneToOneChatRoom(userId, myUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신과는 1:1 채팅방을 만들 수 없습니다.");

        verify(chatRoomService, never()).getOrCreateOneToOneChatRoom(any(), any());
        verify(chatListService, never()).ensureMembership(any(), any());
    }

    @Test
    @DisplayName("그룹 채팅방 생성 성공 시 UUID를 정리하고 중복/본인을 제외한 참여자에게 이벤트를 보낸다")
    void createGroupChatRoomShouldNormalizeMembersAndBroadcastEvents() {
        Long requesterId = 1L;
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(
                "  새 그룹방  ",
                Arrays.asList(" uuid-2 ", "uuid-3", "uuid-2", "my-uuid", " ", null)
        );
        ChatRoom room = chatRoom(200L, RoomType.GROUP, "새 그룹방", null);
        ChatListItemResponse item1 = new ChatListItemResponse(200L, "me", 0, "", null, null);
        ChatListItemResponse item2 = new ChatListItemResponse(200L, "two", 0, "", null, null);
        ChatListItemResponse item3 = new ChatListItemResponse(200L, "three", 0, "", null, null);
        ChatListUpsertEvent event1 = mock(ChatListUpsertEvent.class);
        ChatListUpsertEvent event2 = mock(ChatListUpsertEvent.class);
        ChatListUpsertEvent event3 = mock(ChatListUpsertEvent.class);

        when(userService.getUserIdByUserUuid("uuid-2")).thenReturn(2L);
        when(userService.getUserIdByUserUuid("uuid-3")).thenReturn(3L);
        when(userService.getUserIdByUserUuid("my-uuid")).thenReturn(requesterId);
        when(chatRoomService.createGroupChatRoom("새 그룹방", requesterId)).thenReturn(room);
        when(chatListService.getChatListItem(200L, requesterId)).thenReturn(item1);
        when(chatListService.getChatListItem(200L, 2L)).thenReturn(item2);
        when(chatListService.getChatListItem(200L, 3L)).thenReturn(item3);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(item1)).thenReturn(event1);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(item2)).thenReturn(event2);
        when(chatListUpsertEventMapper.toChatListUpsertEvent(item3)).thenReturn(event3);

        CreateChatRoomResponse response = target.createGroupChatRoom(requesterId, request);

        assertThat(response.roomId()).isEqualTo(200L);
        assertThat(response.roomType()).isEqualTo("GROUP");
        assertThat(response.title()).isEqualTo("새 그룹방");
        verify(chatRoomMemberService).ensureMembership(requesterId, 200L);
        verify(chatRoomMemberService).ensureMembership(2L, 200L);
        verify(chatRoomMemberService).ensureMembership(3L, 200L);
        verify(chatListService).ensureMembership(200L, requesterId);
        verify(chatListService).ensureMembership(200L, 2L);
        verify(chatListService).ensureMembership(200L, 3L);
        verify(chatListEventBroadcaster).broadcastUpsertToUser(requesterId, event1);
        verify(chatListEventBroadcaster).broadcastUpsertToUser(2L, event2);
        verify(chatListEventBroadcaster).broadcastUpsertToUser(3L, event3);
    }

    @Test
    @DisplayName("그룹 채팅방 생성 실패 시 본인을 제외한 유효 멤버가 2명 미만이면 예외가 발생한다")
    void createGroupChatRoomShouldRejectWhenValidInviteesAreLessThanTwo() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest("방", List.of("uuid-2", "uuid-2", " "));

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방은 본인을 제외한 2명 이상의 멤버가 필요합니다.");

        verify(chatRoomService, never()).createGroupChatRoom(any(), any());
    }


    @Test
    @DisplayName("재연결 후 누락 메시지 조회 성공 시 afterMessageId 이후 메시지를 messageId 오름차순으로 반환한다")
    void getMessagesAfterShouldReturnSortedMessagesAfterCursor() {
        Long roomId = 300L;
        Long userId = 1L;
        ChatRoom room = chatRoom(roomId, RoomType.GROUP, "room", 30L);

        LocalDateTime sameTime = LocalDateTime.of(2026, 6, 11, 12, 0);

        ChatMessageItemResponse id20 = messageItem(
                20L,
                "c20",
                2L,
                "u2",
                "sender2",
                "TEXT",
                "m20",
                sameTime,
                0
        );

        ChatMessageItemResponse id10 = messageItem(
                10L,
                "c10",
                3L,
                "u3",
                "sender3",
                "TEXT",
                "m10",
                sameTime,
                0
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);

        when(chatMessageService.getMessagesAfter(roomId, 5L, 100))
                .thenReturn(new SliceImpl<>(List.of(id20, id10), PageRequest.of(0, 100), false));

        when(userDisplayNameService.resolveDisplayNamesBulk(eq(userId), anyList()))
                .thenReturn(Map.of(2L, "별칭2"));

        ChatMessageCatchUpResponse response = target.getMessagesAfter(roomId, userId, 5L, 999);

        verify(chatRoomQueryService).validateMemberOrThrow(roomId, userId);
        verify(chatMessageService).getMessagesAfter(roomId, 5L, 100);
        verify(userDisplayNameService).resolveDisplayNamesBulk(eq(userId), anyList());

        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.messages())
                .extracting(ChatMessageResponse::messageId)
                .containsExactly(10L, 20L);

        assertThat(response.messages().get(1).sender().senderNickname())
                .isEqualTo("별칭2");

        assertThat(response.hasMore()).isFalse();
        assertThat(response.lastMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("재연결 후 누락 메시지 조회 성공 시 결과가 비어 있어도 afterMessageId를 lastMessageId로 반환한다")
    void getMessagesAfterShouldReturnEmptyResponseWithAfterMessageIdWhenNoMessages() {
        Long roomId = 301L;
        Long userId = 1L;
        ChatRoom room = chatRoom(roomId, RoomType.GROUP, "room", 30L);
        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getMessagesAfter(roomId, 50L, 1))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 1), false));
        when(userDisplayNameService.resolveDisplayNamesBulk(userId, List.of())).thenReturn(Map.of());

        ChatMessageCatchUpResponse response = target.getMessagesAfter(roomId, userId, 50L, 0);

        assertThat(response.messages()).isEmpty();
        assertThat(response.lastMessageId()).isEqualTo(50L);
        assertThat(response.hasMore()).isFalse();
    }


    @Test
    @DisplayName("재연결 후 누락 메시지 조회 성공 시 afterMessageId가 null이면 null cursor로 최신 catch-up을 조회한다")
    void getMessagesAfterShouldPassNullCursorWhenAfterMessageIdIsNull() {
        Long roomId = 302L;
        Long userId = 1L;
        ChatRoom room = chatRoom(roomId, RoomType.GROUP, "room", 30L);
        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getMessagesAfter(roomId, null, 20))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));
        when(userDisplayNameService.resolveDisplayNamesBulk(userId, List.of())).thenReturn(Map.of());

        ChatMessageCatchUpResponse response = target.getMessagesAfter(roomId, userId, null, 20);

        verify(chatMessageService).getMessagesAfter(roomId, null, 20);
        assertThat(response.messages()).isEmpty();
        assertThat(response.lastMessageId()).isNull();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    @DisplayName("재연결 후 누락 메시지 조회 실패 시 채팅방 멤버가 아니면 메시지를 조회하지 않는다")
    void getMessagesAfterShouldStopWhenMembershipValidationFails() {
        doThrow(new IllegalArgumentException("not member")).when(chatRoomQueryService).validateMemberOrThrow(300L, 1L);

        assertThatThrownBy(() -> target.getMessagesAfter(300L, 1L, 5L, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not member");

        verify(chatMessageService, never()).getMessagesAfter(any(), any(), anyInt());
    }

    private ChatMessageItemResponse messageItem(
            Long messageId,
            String clientMessageId,
            Long senderId,
            String senderUuid,
            String senderNickname,
            String messageType,
            String content,
            LocalDateTime createdAt,
            int unreadCount
    ) {
        return new ChatMessageItemResponse(
                messageId,
                clientMessageId,
                senderId,
                senderUuid,
                senderNickname,
                "profile-" + senderId,
                messageType,
                content,
                createdAt,
                unreadCount
        );
    }

    private ChatRoom chatRoom(Long id, RoomType roomType, String name, Long lastMessageId) {
        return ChatRoom.builder()
                .id(id)
                .roomType(roomType)
                .name(name)
                .lastMessageId(lastMessageId)
                .createdBy(User.builder().id(1L).uuid("creator").nickname("creator").build())
                .build();
    }
}
