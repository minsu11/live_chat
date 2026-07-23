package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroommember.service.ChatRoomMemberQueryService;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatReadUpdatedEventMapper;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.service.UserService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageReadBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatReadFacadeServiceImplTest {
    private ChatReadService chatReadService;
    private ChatRoomQueryService chatRoomQueryService;
    private ChatListService chatListService;
    private ChatMessageReadBroadcaster readBroadcaster;
    private UserService userService;
    private ChatMessageService chatMessageService;
    private ChatListEventBroadcaster listBroadcaster;
    private ChatReadUpdatedEventMapper readEventMapper;
    private ChatListUpsertEventMapper listEventMapper;
    private ChatMetadataRedisService redisService;
    private ChatRoomMemberQueryService memberQueryService;
    private ChatReadFacadeServiceImpl facade;

    @BeforeEach
    void setUp() {
        chatReadService = mock(ChatReadService.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        chatListService = mock(ChatListService.class);
        readBroadcaster = mock(ChatMessageReadBroadcaster.class);
        userService = mock(UserService.class);
        chatMessageService = mock(ChatMessageService.class);
        listBroadcaster = mock(ChatListEventBroadcaster.class);
        readEventMapper = mock(ChatReadUpdatedEventMapper.class);
        listEventMapper = mock(ChatListUpsertEventMapper.class);
        redisService = mock(ChatMetadataRedisService.class);
        memberQueryService = mock(ChatRoomMemberQueryService.class);
        facade = new ChatReadFacadeServiceImpl(chatReadService, chatRoomQueryService, chatListService, readBroadcaster,
                userService, chatMessageService, listBroadcaster, readEventMapper, listEventMapper, redisService, memberQueryService);
    }

    @Test
    @DisplayName("읽음 이벤트 성공 시 lastRead 갱신, 메시지 unread 이벤트, 채팅목록 upsert 이벤트를 전파한다")
    void readShouldUpdateReadStateAndBroadcastReadAndChatListEvents() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
        ChatListItemResponse item = new ChatListItemResponse(10L, "room", 0, "preview", now, now);
        ChatListUpsertEvent listEvent = new ChatListUpsertEvent(10L, "UPSERT", "room", 0, "preview", now, now);
        List<UpdatedMessageUnreadCount> counts = List.of(new UpdatedMessageUnreadCount(100L, 1));
        ChatReadUpdatedEvent readEvent = new ChatReadUpdatedEvent(10L, "READ_UPDATED", "reader-uuid", 100L, counts);
        when(userService.getUuidByUserId(1L)).thenReturn("reader-uuid");
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of(1L, 2L));
        when(redisService.getAllMembersLastReadId(10L, List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 50L));
        when(chatMessageService.calculateUnreadCountsWithRedis(10L, 100L, Map.of(1L, 100L, 2L, 50L), 2)).thenReturn(counts);
        when(readEventMapper.toChatReadUpdatedEvent(10L, "reader-uuid", 100L, counts)).thenReturn(readEvent);
        when(chatListService.getChatListItem(10L, 1L)).thenReturn(item);
        when(listEventMapper.toChatListUpsertEvent(10L, "room", 0, "preview", now, now)).thenReturn(listEvent);

        facade.read(new ChatReadRequest(10L, 100L), 1L);

        verify(chatRoomQueryService).validateMemberOrThrow(10L, 1L);
        verify(chatReadService).markAsRead(10L, 1L, 100L);
        verify(readBroadcaster).readRoomBroadcast(10L, readEvent);
        verify(listBroadcaster).broadcastUpsertToUser(1L, listEvent);
    }

    @Test
    @DisplayName("읽음 이벤트 실패 시 멤버가 아니면 읽음 저장과 이벤트 전파를 하지 않는다")
    void readShouldStopWhenMembershipValidationFails() {
        doThrow(new IllegalArgumentException("not member")).when(chatRoomQueryService).validateMemberOrThrow(10L, 1L);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not member");

        verify(chatReadService, never()).markAsRead(any(), any(), any());
        verify(readBroadcaster, never()).readRoomBroadcast(any(), any());
        verify(listBroadcaster, never()).broadcastUpsertToUser(any(), any());
    }

    @Test
    @DisplayName("채팅방 진입 읽음 처리 성공 시 최신 메시지가 없으면 메시지 unread 이벤트는 전파하지 않는다")
    void markAsReadOnEnterShouldSkipReadUpdatedEventWhenLatestMessageIsNull() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
        ChatListItemResponse item = new ChatListItemResponse(10L, "room", 0, "preview", now, now);
        ChatListUpsertEvent listEvent = new ChatListUpsertEvent(10L, "UPSERT", "room", 0, "preview", now, now);
        when(chatListService.getChatListItem(10L, 1L)).thenReturn(item);
        when(listEventMapper.toChatListUpsertEvent(10L, "room", 0, "preview", now, now)).thenReturn(listEvent);

        facade.markAsReadOnEnter(10L, 1L, null);

        verify(chatReadService).markAsReadOnEnter(10L, 1L, null);
        verify(listBroadcaster).broadcastUpsertToUser(1L, listEvent);
        verify(readBroadcaster, never()).readRoomBroadcast(any(), any());
    }
}
