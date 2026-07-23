package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatMetadataWriteModeTest {

    private RedisTemplate<String, String> redisTemplate;
    private ChatListRepository chatListRepository;
    private ChatRoomRepository chatRoomRepository;
    private ChatMetadataRedisService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        chatListRepository = mock(ChatListRepository.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        service = new ChatMetadataRedisService(redisTemplate, chatListRepository, chatRoomRepository);
        ReflectionTestUtils.setField(service, "writeMode", "sync-db");
    }

    @Test
    @DisplayName("sync-db 모드에서는 채팅방 메타데이터를 Redis가 아니라 DB에 즉시 반영한다")
    void updateRoomMetaShouldWriteDirectlyToDbInSyncMode() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 14, 0);

        service.updateRoomMeta(10L, 100L, "메시지", createdAt);

        verify(chatRoomRepository).syncRoomMetaFromRedis(10L, 100L, "메시지", createdAt);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("sync-db 모드에서는 지정된 수신자의 unread count만 1 증가시킨다")
    void incrementUnreadCountShouldUpdateSingleUserInSyncMode() {
        service.incrementUnreadCount(10L, 2L);

        verify(chatListRepository).addUnreadCountBatch(10L, 2L, 1);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("sync-db 모드에서는 읽음 메타데이터를 DB에 즉시 반영한다")
    void markAsReadShouldWriteDirectlyToDbInSyncMode() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 7, 20, 14, 10);

        service.markAsRead(10L, 1L, 100L, openedAt);

        verify(chatListRepository).syncUserMetaFromRedis(10L, 1L, 0, 100L, openedAt);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("sync-db 모드에서는 멤버 읽음 위치를 Redis 없이 DB에서 조회한다")
    void getAllMembersLastReadIdShouldReadFromDbInSyncMode() {
        ChatList first = mock(ChatList.class);
        ChatList second = mock(ChatList.class);
        when(first.getLastReadMessageId()).thenReturn(90L);
        when(second.getLastReadMessageId()).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 1L)).thenReturn(Optional.of(first));
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(Optional.of(second));

        Map<Long, Long> result = service.getAllMembersLastReadId(10L, List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 90L).containsEntry(2L, 0L);
        verifyNoInteractions(redisTemplate);
    }
}
