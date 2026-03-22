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
import com.chat_server.user.service.UserService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private ChatRoomFacadeServiceImpl target;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatListService = mock(ChatListService.class);
        chatRoomMemberService = mock(ChatRoomMemberService.class);
        chatMessageService = mock(ChatMessageService.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        userService = mock(UserService.class);

        target = new ChatRoomFacadeServiceImpl(
                chatRoomService,
                chatListService,
                chatRoomMemberService,
                chatMessageService,
                chatRoomQueryService,
                userService
        );
    }

    @Test
    void enterChatRoom_커서페이지_정렬과_nextCursor를_반환한다() {
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

        ChatMessageItemResponse newer = new ChatMessageItemResponse(20L, 1L, "u1", "TEXT", "new", t2);
        ChatMessageItemResponse older = new ChatMessageItemResponse(10L, 2L, "u2", "TEXT", "old", t1);

        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
                List.of(newer, older),
                PageRequest.of(0, limit),
                true
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class)))
                .thenReturn(slice);

        long cursorMillis = t2.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        String cursor = ChatMessageCursorCodec.encode(cursorMillis, 20L);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, cursor, limit);

        verify(chatRoomQueryService).validateMemberOrThrow(roomId, userId);
        verify(chatMessageService).getEnterMessagesByCursor(eq(roomId), eq(limit), any(ChatMessageCursorKey.class));

        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.roomType()).isEqualTo("DM");
        assertThat(response.title()).isEqualTo("테스트방");
        assertThat(response.messages()).extracting(ChatMessageItemResponse::messageId)
                .containsExactly(10L, 20L);

        ChatMessageCursorKey next = ChatMessageCursorCodec.decode(response.nextCursor());
        assertThat(next).isNotNull();
        assertThat(next.lastMessageId()).isEqualTo(10L);
    }

    @Test
    void enterChatRoom_다음페이지없으면_nextCursor는_null() {
        Long roomId = 1L;
        Long userId = 2L;

        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("group")
                .createdBy(User.builder().id(1L).build())
                .build();

        Slice<ChatMessageItemResponse> slice = new SliceImpl<>(
                List.of(new ChatMessageItemResponse(1L, 1L, "u", "TEXT", "hi", LocalDateTime.now())),
                PageRequest.of(0, 50),
                false
        );

        when(chatRoomQueryService.getRoomOrThrow(roomId)).thenReturn(room);
        when(chatMessageService.getEnterMessagesByCursor(eq(roomId), eq(50), Mockito.isNull()))
                .thenReturn(slice);

        ChatRoomEnterResponse response = target.enterChatRoom(roomId, userId, null, 50);

        assertThat(response.nextCursor()).isNull();
    }
}
