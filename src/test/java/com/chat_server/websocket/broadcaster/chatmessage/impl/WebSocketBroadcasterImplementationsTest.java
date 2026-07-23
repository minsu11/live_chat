package com.chat_server.websocket.broadcaster.chatmessage.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatroommember.service.ChatRoomMemberQueryService;
import com.chat_server.redis.service.RedisPublisher;
import com.chat_server.websocket.properties.WebSocketProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class WebSocketBroadcasterImplementationsTest {

    private RedisPublisher redisPublisher;
    private WebSocketProperties properties;

    @BeforeEach
    void setUp() {
        redisPublisher = mock(RedisPublisher.class);
        properties = new WebSocketProperties();
        properties.setSubPrefix("/sub");

        WebSocketProperties.Chat chat = new WebSocketProperties.Chat();
        chat.setRoomPath("/rooms");
        chat.setChatList("/chat-list");
        chat.setChatNotification("/notifications");
        properties.setChat(chat);
    }

    /** 사용자별 메시지 큐 목적지가 정책에 맞게 조합되는지 검증한다. */
    @Test
    @DisplayName("채팅 메시지 브로드캐스트 성공 - 사용자 전용 방 경로로 Redis 발행한다")
    void chatMessageBroadcasterShouldPublishToUserRoomDestination() {
        ChatMessageResponse response = mock(ChatMessageResponse.class);
        when(response.roomId()).thenReturn(10L);
        ChatMessageBroadCasterImpl broadcaster =
                new ChatMessageBroadCasterImpl(redisPublisher, properties);

        broadcaster.broadcastMessage(1L, response);

        verify(redisPublisher).publish("/user/1/sub/rooms/10", response);
    }

    /** 방 전체 relay 목적지가 사용자 prefix 없이 생성되는지 검증한다. */
    @Test
    @DisplayName("채팅 메시지 relay 성공 - 방 구독 경로로 Redis 발행한다")
    void chatMessageBroadcasterShouldRelayToRoomDestination() {
        ChatMessageResponse response = mock(ChatMessageResponse.class);
        when(response.roomId()).thenReturn(10L);
        ChatMessageBroadCasterImpl broadcaster =
                new ChatMessageBroadCasterImpl(redisPublisher, properties);

        broadcaster.relayRoomBroadcast(response);

        verify(redisPublisher).publish("/sub/rooms/10", response);
    }

    /** 정상 채팅 목록 이벤트를 사용자 전용 경로로 발행하는지 검증한다. */
    @Test
    @DisplayName("채팅 목록 이벤트 성공 - 사용자 전용 chat-list 경로로 발행한다")
    void chatListBroadcasterShouldPublishValidEvent() {
        ChatListUpsertEvent event = mock(ChatListUpsertEvent.class);
        ChatListEventBroadcasterImpl broadcaster =
                new ChatListEventBroadcasterImpl(redisPublisher, properties);

        broadcaster.broadcastUpsertToUser(2L, event);

        verify(redisPublisher).publish("/user/2/sub/chat-list", event);
    }

    /** userId 또는 event가 null인 validation 경로에서는 발행하지 않는지 검증한다. */
    @Test
    @DisplayName("채팅 목록 이벤트 validation - userId 또는 event가 null이면 발행하지 않는다")
    void chatListBroadcasterShouldSkipNullArguments() {
        ChatListEventBroadcasterImpl broadcaster =
                new ChatListEventBroadcasterImpl(redisPublisher, properties);
        ChatListUpsertEvent event = mock(ChatListUpsertEvent.class);

        broadcaster.broadcastUpsertToUser(null, event);
        broadcaster.broadcastUpsertToUser(1L, null);

        verifyNoInteractions(redisPublisher);
    }

    /** 채팅 알림 목적지가 설정값에 맞게 조합되는지 검증한다. */
    @Test
    @DisplayName("채팅 알림 성공 - 사용자 전용 notification 경로로 발행한다")
    void notificationBroadcasterShouldPublishToUserDestination() {
        ChatNotificationEvent event = mock(ChatNotificationEvent.class);
        ChatNotificationBroadcasterImpl broadcaster =
                new ChatNotificationBroadcasterImpl(redisPublisher, properties);

        broadcaster.broadcastToUser(3L, event);

        verify(redisPublisher).publish("/user/3/sub/notifications", event);
    }

    /** 읽음 이벤트가 방의 모든 멤버에게 각각 발행되는지 검증한다. */
    @Test
    @DisplayName("읽음 이벤트 성공 - 채팅방 모든 멤버의 사용자 전용 read 경로로 발행한다")
    void readBroadcasterShouldPublishToEveryMember() {
        ChatRoomMemberQueryService memberQueryService = mock(ChatRoomMemberQueryService.class);
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of(1L, 2L, 3L));
        ChatReadUpdatedEvent event = mock(ChatReadUpdatedEvent.class);
        ChatMessageReadBroadcasterImpl broadcaster =
                new ChatMessageReadBroadcasterImpl(redisPublisher, properties, memberQueryService);

        broadcaster.readRoomBroadcast(10L, event);

        verify(redisPublisher).publish("/user/1/sub/rooms/10/read", event);
        verify(redisPublisher).publish("/user/2/sub/rooms/10/read", event);
        verify(redisPublisher).publish("/user/3/sub/rooms/10/read", event);
    }

    /** 멤버가 없는 방에서는 Redis 발행이 발생하지 않는 경계값을 검증한다. */
    @Test
    @DisplayName("읽음 이벤트 경계값 - 채팅방 멤버가 없으면 발행하지 않는다")
    void readBroadcasterShouldSkipWhenMemberListIsEmpty() {
        ChatRoomMemberQueryService memberQueryService = mock(ChatRoomMemberQueryService.class);
        when(memberQueryService.getMemberUserIds(10L)).thenReturn(List.of());
        ChatMessageReadBroadcasterImpl broadcaster =
                new ChatMessageReadBroadcasterImpl(redisPublisher, properties, memberQueryService);

        broadcaster.readRoomBroadcast(10L, mock(ChatReadUpdatedEvent.class));

        verifyNoInteractions(redisPublisher);
    }
}
