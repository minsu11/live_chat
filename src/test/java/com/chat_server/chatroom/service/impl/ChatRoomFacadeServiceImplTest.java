package com.chat_server.chatroom.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

class ChatRoomFacadeServiceImplTest {

    private ChatRoomService chatRoomService;
    private ChatListService chatListService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatMessageService chatMessageService;
    private ChatRoomQueryService chatRoomQueryService;
    private UserService userService;
    private UserDisplayNameService userDisplayNameService;
    private ChatReadService chatReadService;

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
        chatReadService = mock(ChatReadService.class);

        target = new ChatRoomFacadeServiceImpl(
                chatRoomService,
                chatListService,
                chatRoomMemberService,
                chatMessageService,
                chatRoomQueryService,
                userService,
                userDisplayNameService,
                chatReadService
        );

        when(chatListService.getCustomRoomName(Mockito.anyLong(), Mockito.anyLong())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("hasNext=true면 nextCursor를 생성하고 메시지를 시간순으로 정렬한다")
    void shouldReturnSortedMessagesAndNextCursorWhenHasNext() {
        Long roomId = 10L;
        Long userId = 99L;
        int limit = 50;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.DM)
                .name("테스트방")
                .createdBy(User.builder().id(1L).build())
                .build();

        LocalDateTime t1 = LocalDateTime.of(2026, 3, 20, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 3, 20, 10, 1, 0);

        ChatMessageItemResponse newer = new ChatMessageItemResponse(20L, 1L, "uuid","u1", "","TEXT", "new", t2);
        ChatMessageItemResponse older = new ChatMessageItemResponse(10L, 2L, "uuid","u2", "","TEXT", "old", t1);

        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
                List.of(newer, older),
                PageRequest.of(0, limit),
                true
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatRoomQueryService.getMemberId(roomId, userId)).thenReturn(77L);
        when(userDisplayNameService.resolveDisplayName(77L, userId)).thenReturn(java.util.Optional.of("상대 별칭"));
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class)))
                .thenReturn(slice);

        long cursorMillis = t2.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        String cursor = ChatMessageCursorCodec.encode(cursorMillis, 20L);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, cursor, limit);

        verify(chatRoomQueryService).validateMemberOrThrow(roomId, userId);
        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class));

        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.roomType()).isEqualTo("DM");
        assertThat(response.title()).isEqualTo("상대 별칭");
        assertThat(response.messages()).extracting(ChatMessageItemResponse::messageId)
                .containsExactly(10L, 20L);

        ChatMessageCursorKey next = ChatMessageCursorCodec.decode(response.nextCursor());
        assertThat(next).isNotNull();
        assertThat(next.lastMessageId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("hasNext=false면 nextCursor는 null이다")
    void shouldReturnNullNextCursorWhenHasNextIsFalse() {
        Long roomId = 1L;
        Long userId = 2L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("group")
                .createdBy(User.builder().id(1L).build())
                .build();

        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
                List.of(new ChatMessageItemResponse(1L, 1L, "uuid","u","profileUrl", "TEXT", "hi", LocalDateTime.now())),
                PageRequest.of(0, 50),
                false
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(slice);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("limit가 0 이하일 때는 1로 보정해서 조회한다")
    void shouldClampLimitToOneWhenLimitIsNonPositive() {
        Long roomId = 2L;
        Long userId = 3L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(1), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 1), false));

        target.enterChatRoom(roomId, userId, null, 0);

        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(1), Mockito.isNull());
    }

    @Test
    @DisplayName("limit가 100보다 크면 100으로 보정해서 조회한다")
    void shouldClampLimitToHundredWhenLimitIsTooLarge() {
        Long roomId = 3L;
        Long userId = 4L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(100), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 100), false));

        target.enterChatRoom(roomId, userId, null, 999);

        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(100), Mockito.isNull());
    }

    @Test
    @DisplayName("깨진 커서는 null 커서처럼 처리되어 첫 페이지를 조회한다")
    void shouldTreatBrokenCursorAsFirstPage() {
        Long roomId = 4L;
        Long userId = 5L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        target.enterChatRoom(roomId, userId, "broken-cursor", 50);

        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull());
    }

    @Test
    @DisplayName("동일 createdAt인 메시지는 messageId 오름차순으로 정렬된다")
    void shouldSortByMessageIdWhenCreatedAtIsSame() {
        Long roomId = 5L;
        Long userId = 6L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).build())
                .build();

        LocalDateTime same = LocalDateTime.of(2026, 3, 20, 10, 0, 0);
        ChatMessageItemResponse id20 = new ChatMessageItemResponse(20L, 1L, "uuid","u1", "profileUrl","TEXT", "m2", same);
        ChatMessageItemResponse id10 = new ChatMessageItemResponse(10L, 2L, "uuid","u2","profileUrl", "TEXT", "m1", same);

        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
                List.of(id20, id10),
                PageRequest.of(0, 50),
                false
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(slice);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.messages()).extracting(ChatMessageItemResponse::messageId)
                .containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("입력 커서는 디코딩되어 서비스에 전달된다")
    void shouldPassDecodedCursorToService() {
        Long roomId = 6L;
        Long userId = 7L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).build())
                .build();

        LocalDateTime now = LocalDateTime.of(2026, 3, 20, 10, 10, 0);
        long millis = now.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        String cursor = ChatMessageCursorCodec.encode(millis, 111L);

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), any(ChatMessageCursorKey.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        target.enterChatRoom(roomId, userId, cursor, 50);

        ArgumentCaptor<ChatMessageCursorKey> captor = ArgumentCaptor.forClass(ChatMessageCursorKey.class);
        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(50), captor.capture());
        assertThat(captor.getValue().lastMessageId()).isEqualTo(111L);
        assertThat(captor.getValue().lastMessageAtEpochMillis()).isEqualTo(millis);
    }

    @Test
    @DisplayName("DM 방은 표시 이름이 없으면 방 제목으로 fallback 한다")
    void shouldFallbackToRoomTitleWhenDmDisplayNameIsMissing() {
        Long roomId = 7L;
        Long userId = 8L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.DM)
                .name("")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatRoomQueryService.getMemberId(roomId, userId)).thenReturn(88L);
        when(userDisplayNameService.resolveDisplayName(88L, userId)).thenReturn(java.util.Optional.empty());
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.title()).isEmpty();
    }

    @Test
    @DisplayName("DM 이 아닌 방은 기존 room.name을 title로 사용한다")
    void shouldUseRoomNameWhenRoomTypeIsNotDm() {
        Long roomId = 8L;
        Long userId = 9L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("그룹방")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.title()).isEqualTo("그룹방");
        verify(chatListService).getCustomRoomName(roomId, userId);
        verify(chatRoomQueryService, Mockito.never()).getMemberId(any(), any());
        verify(userDisplayNameService, Mockito.never()).resolveDisplayName(any(), any());
    }

    @Test
    @DisplayName("GROUP 방은 chat list custom name이 있으면 해당 값을 title로 사용한다")
    void shouldUseCustomNameWhenGroupHasCustomName() {
        Long roomId = 11L;
        Long userId = 12L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("원래 그룹방 이름")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatListService.getCustomRoomName(roomId, userId)).thenReturn(Optional.of("내가 바꾼 그룹방 이름"));
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.title()).isEqualTo("내가 바꾼 그룹방 이름");
    }

    @Test
    @DisplayName("OPEN 방은 room title을 그대로 사용한다")
    void shouldUseRoomTitleWhenOpenRoom() {
        Long roomId = 13L;
        Long userId = 14L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.OPEN)
                .name("오픈 채팅방")
                .createdBy(User.builder().id(1L).build())
                .build();

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.title()).isEqualTo("오픈 채팅방");
        verify(chatListService, Mockito.never()).getCustomRoomName(roomId, userId);
        verify(chatRoomQueryService, Mockito.never()).getMemberId(any(), any());
        verify(userDisplayNameService, Mockito.never()).resolveDisplayName(any(), any());
    }

}
