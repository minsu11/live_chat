package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.dto.response.ChatUnreadCountRow;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatListServiceImplTest {
    private ChatListRepository chatListRepository;
    private ChatRoomDisplayResolver displayResolver;
    private ChatMetadataRedisService redisService;
    private ChatListServiceImpl service;

    @BeforeEach
    void setUp() {
        chatListRepository = mock(ChatListRepository.class);
        displayResolver = mock(ChatRoomDisplayResolver.class);
        redisService = mock(ChatMetadataRedisService.class);
        service = new ChatListServiceImpl(chatListRepository, displayResolver, mock(CustomProperties.class),
                mock(ChatRoomRepository.class), mock(UserRepository.class), redisService);
    }

    @Test
    @DisplayName("채팅 목록 조회 성공 시 Redis unreadCount와 방 메타를 목록 응답에 반영하고 orderAt 기준으로 정렬한다")
    void getChatRoomListsByCursorShouldMergeRedisUnreadAndRoomMeta() {
        LocalDateTime oldTime = LocalDateTime.of(2026, 6, 11, 10, 0);
        LocalDateTime newTime = LocalDateTime.of(2026, 6, 11, 11, 0);
        ChatRoomListResponse room1 = new ChatRoomListResponse(1L, "old", 9, "old", oldTime, oldTime, false);
        ChatRoomListResponse room2 = new ChatRoomListResponse(2L, "new", 0, "new", oldTime, oldTime, true);
        when(chatListRepository.getChatRoomListByCursor(eq(10L), eq(50), isNull()))
                .thenReturn(new SliceImpl<>(List.of(room1, room2), PageRequest.of(0, 50), false));
        when(redisService.getUnreadCount(1L, 10L)).thenReturn(3);
        when(redisService.getUnreadCount(2L, 10L)).thenReturn(0);
        when(redisService.getRoomMeta(1L)).thenReturn(null);
        when(redisService.getRoomMeta(2L)).thenReturn(new ChatRoomMetaDto(200L, "redis-preview", newTime));
        when(displayResolver.resolveTitle(1L, 10L)).thenReturn("room-1");
        when(displayResolver.resolveTitle(2L, 10L)).thenReturn("room-2");

        var response = service.getChatRoomListsByCursor(10L, 50, null);

        assertThat(response.items()).extracting(ChatRoomListResponse::roomId).containsExactly(2L, 1L);
        assertThat(response.items().get(0).lastMessagePreview()).isEqualTo("redis-preview");
        assertThat(response.items().get(1).unreadCount()).isEqualTo(3);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("읽음 처리 성공 시 멤버십 확인 후 Redis lastReadMessageId와 unreadCount 초기화를 호출한다")
    void markAsReadShouldUpdateRedisWhenMembershipExists() {
        when(chatListRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true);

        int updated = service.markAsRead(10L, 1L, 100L);

        assertThat(updated).isEqualTo(1);
        verify(redisService).markAsRead(eq(10L), eq(1L), eq(100L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("읽음 처리 실패 시 멤버십이 없으면 Redis를 호출하지 않고 0을 반환한다")
    void markAsReadShouldReturnZeroWhenMembershipMissing() {
        when(chatListRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(false);

        int updated = service.markAsRead(10L, 1L, 100L);

        assertThat(updated).isZero();
        verify(redisService, never()).markAsRead(any(), any(), any(), any());
    }

    @Test
    @DisplayName("채팅방 unreadCount map 조회 시 없는 사용자는 0으로 채운다")
    void getUnreadCountMapShouldFillMissingUsersWithZero() {
        when(chatListRepository.findUnreadCountRows(10L, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new ChatUnreadCountRow(1L, 5), new ChatUnreadCountRow(2L, null)));

        Map<Long, Integer> result = service.getUnreadCountMap(10L, List.of(1L, 2L, 3L));

        assertThat(result).containsEntry(1L, 5).containsEntry(2L, 0).containsEntry(3L, 0);
    }

    @Test
    @DisplayName("채팅방 목록 단건 조회 성공 시 채팅방 목록 unreadCount는 Redis의 사용자별 unreadCount를 사용한다")
    void getChatListItemShouldUseRoomListUnreadCountFromRedis() {
        LocalDateTime dbTime = LocalDateTime.of(2026, 6, 11, 10, 0);
        LocalDateTime redisTime = LocalDateTime.of(2026, 6, 11, 11, 0);
        when(chatListRepository.findChatListItem(10L, 1L))
                .thenReturn(Optional.of(new ChatListItemResponse(10L, "db", 99, "db-preview", dbTime, dbTime)));
        when(redisService.getUnreadCount(10L, 1L)).thenReturn(7);
        when(redisService.getRoomMeta(10L)).thenReturn(new ChatRoomMetaDto(200L, "redis-preview", redisTime));
        when(displayResolver.resolveTitle(10L, 1L)).thenReturn("resolved-title");

        ChatListItemResponse result = service.getChatListItem(10L, 1L);

        assertThat(result.unreadCount()).isEqualTo(7);
        assertThat(result.lastMessagePreview()).isEqualTo("redis-preview");
        assertThat(result.displayName()).isEqualTo("resolved-title");
        assertThat(result.orderAt()).isEqualTo(redisTime);
    }
}
