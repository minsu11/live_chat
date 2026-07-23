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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatReadFacadeServiceImplFailureTest {

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

        facade = new ChatReadFacadeServiceImpl(
                chatReadService,
                chatRoomQueryService,
                chatListService,
                readBroadcaster,
                userService,
                chatMessageService,
                listBroadcaster,
                readEventMapper,
                listEventMapper,
                redisService,
                memberQueryService
        );
    }

    /**
     * 읽음 요청 객체가 null인 validation 실패 경로를 검증한다.
     * 요청 값을 읽기 전에 어떤 서비스나 브로드캐스터도 호출되면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 validation 실패 - 요청 객체가 null이면 즉시 예외가 발생하고 외부 기능을 호출하지 않는다")
    void readShouldFailImmediatelyWhenRequestIsNull() {
        assertThatThrownBy(() -> facade.read(null, 1L))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(chatRoomQueryService, chatReadService, readBroadcaster, listBroadcaster);
    }

    /**
     * 멤버 검증은 성공했지만 읽음 상태 저장이 실패하는 예외 경로를 검증한다.
     * 저장되지 않은 읽음 상태를 기준으로 메시지 및 채팅 목록 이벤트를 전파하면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 실패 - 읽음 상태 저장 중 예외가 발생하면 모든 실시간 이벤트 전파를 중단한다")
    void readShouldStopWhenReadStateUpdateFails() {
        RuntimeException updateFailure = new RuntimeException("read state update failed");
        doThrow(updateFailure).when(chatReadService).markAsRead(10L, 1L, 100L);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(updateFailure);

        verify(chatRoomQueryService).validateMemberOrThrow(10L, 1L);
        verifyNoInteractions(userService, memberQueryService, redisService, chatMessageService,
                readEventMapper, readBroadcaster, chatListService, listEventMapper, listBroadcaster);
    }

    /**
     * 사용자 UUID 조회가 실패하는 예외 경로를 검증한다.
     * 읽음 상태 저장은 완료됐지만 unread 계산과 어떤 브로드캐스트도 진행되면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 실패 - 읽은 사용자 UUID 조회가 실패하면 unread 계산과 이벤트 전파를 중단한다")
    void readShouldStopWhenReaderUuidLookupFails() {
        RuntimeException userLookupFailure = new RuntimeException("user not found");
        when(userService.getUuidByUserId(1L)).thenThrow(userLookupFailure);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(userLookupFailure);

        verify(chatReadService).markAsRead(10L, 1L, 100L);
        verifyNoInteractions(memberQueryService, redisService, chatMessageService,
                readEventMapper, readBroadcaster, chatListService, listEventMapper, listBroadcaster);
    }

    /**
     * 채팅방 참여자 목록 조회가 실패하는 예외 경로를 검증한다.
     * Redis 읽음 맵 조회와 메시지 unread 계산, 이벤트 전파가 이어지면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 실패 - 채팅방 참여자 조회가 실패하면 Redis 조회와 이벤트 전파를 중단한다")
    void readShouldStopWhenMemberLookupFails() {
        when(userService.getUuidByUserId(1L)).thenReturn("reader-uuid");
        RuntimeException memberFailure = new RuntimeException("member lookup failed");
        when(memberQueryService.getMemberUserIds(10L)).thenThrow(memberFailure);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(memberFailure);

        verifyNoInteractions(redisService, chatMessageService, readEventMapper,
                readBroadcaster, chatListService, listEventMapper, listBroadcaster);
    }

    /**
     * 참여자별 마지막 읽음 ID를 Redis에서 조회하지 못한 장애 경로를 검증한다.
     * 불완전한 읽음 정보로 unreadCount를 계산하거나 이벤트를 전파하면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 실패 - Redis 읽음 맵 조회가 실패하면 unread 계산과 이벤트 전파를 중단한다")
    void readShouldStopWhenRedisReadMapLookupFails() {
        when(userService.getUuidByUserId(1L)).thenReturn("reader-uuid");
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of(1L, 2L));
        RuntimeException redisFailure = new RuntimeException("redis unavailable");
        when(redisService.getAllMembersLastReadId(10L, List.of(1L, 2L))).thenThrow(redisFailure);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(redisFailure);

        verifyNoInteractions(chatMessageService, readEventMapper, readBroadcaster,
                chatListService, listEventMapper, listBroadcaster);
    }

    /**
     * 메시지별 unreadCount 계산 중 예외가 발생하는 경로를 검증한다.
     * 계산 결과가 없으므로 READ_UPDATED 이벤트 생성과 채팅 목록 갱신을 시작하면 안 된다.
     */
    @Test
    @DisplayName("읽음 처리 실패 - 메시지 unread 계산이 실패하면 읽음 이벤트와 채팅 목록 이벤트를 전파하지 않는다")
    void readShouldStopWhenUnreadCalculationFails() {
        Map<Long, Long> readMap = Map.of(1L, 100L, 2L, 50L);
        when(userService.getUuidByUserId(1L)).thenReturn("reader-uuid");
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of(1L, 2L));
        when(redisService.getAllMembersLastReadId(10L, List.of(1L, 2L))).thenReturn(readMap);
        RuntimeException calculationFailure = new RuntimeException("unread calculation failed");
        when(chatMessageService.calculateUnreadCountsWithRedis(10L, 100L, readMap, 2))
                .thenThrow(calculationFailure);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(calculationFailure);

        verifyNoInteractions(readEventMapper, readBroadcaster,
                chatListService, listEventMapper, listBroadcaster);
    }

    /**
     * READ_UPDATED 이벤트 전파는 성공했지만 채팅 목록 조회가 실패하는 부분 실패 경로를 검증한다.
     * 메시지 읽음 이벤트는 한 번 전파되고 채팅 목록 UPSERT 이벤트는 전파되지 않아야 한다.
     */
    @Test
    @DisplayName("읽음 처리 부분 실패 - 채팅 목록 조회가 실패하면 READ_UPDATED만 전파되고 목록 UPSERT는 전파하지 않는다")
    void readShouldKeepReadBroadcastAndStopListBroadcastWhenChatListLookupFails() {
        List<UpdatedMessageUnreadCount> counts = List.of(new UpdatedMessageUnreadCount(100L, 1));
        ChatReadUpdatedEvent readEvent = new ChatReadUpdatedEvent(
                10L, "READ_UPDATED", "reader-uuid", 100L, counts);

        when(userService.getUuidByUserId(1L)).thenReturn("reader-uuid");
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of(1L, 2L));
        when(redisService.getAllMembersLastReadId(10L, List.of(1L, 2L)))
                .thenReturn(Map.of(1L, 100L, 2L, 50L));
        when(chatMessageService.calculateUnreadCountsWithRedis(
                10L, 100L, Map.of(1L, 100L, 2L, 50L), 2)).thenReturn(counts);
        when(readEventMapper.toChatReadUpdatedEvent(10L, "reader-uuid", 100L, counts))
                .thenReturn(readEvent);
        RuntimeException listFailure = new RuntimeException("chat list not found");
        when(chatListService.getChatListItem(10L, 1L)).thenThrow(listFailure);

        assertThatThrownBy(() -> facade.read(new ChatReadRequest(10L, 100L), 1L))
                .isSameAs(listFailure);

        verify(readBroadcaster).readRoomBroadcast(10L, readEvent);
        verifyNoInteractions(listEventMapper, listBroadcaster);
    }

    /**
     * 채팅방 최초 진입 시 latestMessageId가 0인 경계값을 검증한다.
     * 채팅 목록 unread는 갱신하지만 메시지 READ_UPDATED 이벤트는 전파하지 않아야 한다.
     */
    @Test
    @DisplayName("채팅방 진입 읽음 validation - 최신 메시지 ID가 0이면 채팅 목록만 갱신하고 메시지 읽음 이벤트는 생략한다")
    void markAsReadOnEnterShouldSkipMessageReadBroadcastWhenLatestMessageIdIsZero() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 14, 0);
        ChatListItemResponse item = new ChatListItemResponse(10L, "room", 0, "", now, now);
        ChatListUpsertEvent listEvent = new ChatListUpsertEvent(
                10L, "UPSERT", "room", 0, "", now, now);

        when(chatListService.getChatListItem(10L, 1L)).thenReturn(item);
        when(listEventMapper.toChatListUpsertEvent(10L, "room", 0, "", now, now))
                .thenReturn(listEvent);

        facade.markAsReadOnEnter(10L, 1L, 0L);

        verify(chatReadService).markAsReadOnEnter(10L, 1L, 0L);
        verify(listBroadcaster).broadcastUpsertToUser(1L, listEvent);
        verifyNoInteractions(userService, memberQueryService, redisService,
                chatMessageService, readEventMapper, readBroadcaster);
    }

    /**
     * 채팅방 진입 시 멤버 검증에 실패하는 경로를 검증한다.
     * 읽음 상태 저장과 채팅 목록/메시지 이벤트 전파가 전혀 수행되지 않아야 한다.
     */
    @Test
    @DisplayName("채팅방 진입 읽음 실패 - 멤버가 아니면 읽음 저장과 모든 이벤트 전파를 중단한다")
    void markAsReadOnEnterShouldStopWhenMembershipValidationFails() {
        RuntimeException forbidden = new RuntimeException("not a member");
        doThrow(forbidden).when(chatRoomQueryService).validateMemberOrThrow(10L, 1L);

        assertThatThrownBy(() -> facade.markAsReadOnEnter(10L, 1L, 100L))
                .isSameAs(forbidden);

        verifyNoInteractions(chatReadService, chatListService, listEventMapper, listBroadcaster,
                userService, memberQueryService, redisService, chatMessageService,
                readEventMapper, readBroadcaster);
    }
}
