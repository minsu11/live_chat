package com.chat_server.redis.service;

import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.*;

class ChatMetadataRedisServiceTest {
    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private SetOperations<String, String> setOperations;
    private ChatListRepository chatListRepository;
    private ChatRoomRepository chatRoomRepository;
    private ChatMetadataRedisService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        setOperations = mock(SetOperations.class);
        chatListRepository = mock(ChatListRepository.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        service = new ChatMetadataRedisService(redisTemplate, chatListRepository, chatRoomRepository);
    }

    @Test
    @DisplayName("읽음 처리 성공 시 Redis에 lastReadMessageId와 unreadCount 0을 저장하고 dirty set에 추가한다")
    void markAsReadShouldStoreLastReadAndResetUnreadCount() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.markAsRead(10L, 1L, 100L, openedAt);

        verify(hashOperations).putAll("chat:room:10:user:1:meta", Map.of(
                "lastReadMessageId", "100",
                "unreadCount", "0",
                "lastOpenedAt", openedAt.toString()
        ));
        verify(setOperations).add("chat:user:dirty", "10:1");
    }

    @Test
    @DisplayName("방 메타 갱신 성공 시 Redis 방 메타와 dirty set을 갱신한다")
    void updateRoomMetaShouldStoreRoomMetaAndDirtySet() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.updateRoomMeta(10L, 100L, "preview", createdAt);

        verify(hashOperations).putAll("chat:room:10:meta", Map.of(
                "lastMessageId", "100",
                "lastMessagePreview", "preview",
                "lastMessageAt", createdAt.toString()
        ));
        verify(setOperations).add("chat:room:dirty", "10");
    }

    @Test
    @DisplayName("읽음 처리 fallback은 DB 동기화 repository를 호출한다")
    void markAsReadFallbackShouldSyncUserMetaToDatabase() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.markAsReadFallback(10L, 1L, 100L, openedAt, new RuntimeException("redis down"));

        verify(chatListRepository).syncUserMetaFromRedis(10L, 1L, 0, 100L, openedAt);
    }

    @Test
    @DisplayName("방 메타 갱신 fallback은 DB 방 메타 동기화 repository를 호출한다")
    void updateRoomMetaFallbackShouldSyncRoomMetaToDatabase() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.updateRoomMetaFallback(10L, 100L, "preview", createdAt, new RuntimeException("redis down"));

        verify(chatRoomRepository).syncRoomMetaFromRedis(10L, 100L, "preview", createdAt);
    }
}
