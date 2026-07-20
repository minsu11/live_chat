package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    /**
     * Redis에 마지막 메시지 ID가 있으면 DB Entity 값보다 Redis 값을 우선 반환하는지 검증한다.
     */
    @Test
    @DisplayName("최근 메시지 조회 성공 - Redis 값이 있으면 Entity의 lastMessageId보다 우선 사용한다")
    void getLatestMessageIdShouldReturnRedisValueWhenPresent() {
        when(hashOperations.get("chat:room:10:meta", "lastMessageId")).thenReturn("200");

        Long result = service.getLatestMessageId(10L, 100L);

        assertThat(result).isEqualTo(200L);
    }

    /**
     * Redis 캐시에 값이 없는 초기 상태에서는 DB Entity가 가진 마지막 메시지 ID를 반환하는지 검증한다.
     */
    @Test
    @DisplayName("최근 메시지 조회 fallback - Redis 값이 없으면 Entity의 lastMessageId를 반환한다")
    void getLatestMessageIdShouldReturnEntityValueWhenCacheIsMissing() {
        when(hashOperations.get("chat:room:10:meta", "lastMessageId")).thenReturn(null);

        Long result = service.getLatestMessageId(10L, 99L);

        assertThat(result).isEqualTo(99L);
    }

    /**
     * 읽음 처리 시 lastReadMessageId와 unreadCount 0을 함께 기록하고 Dirty Set에 등록하는지 검증한다.
     */
    @Test
    @DisplayName("읽음 처리 성공 - Redis에 lastReadMessageId와 unreadCount 0을 저장하고 Dirty Set에 추가한다")
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

    /**
     * 읽음 처리 Redis fallback이 동일한 값을 DB 동기화 쿼리에 전달하는지 검증한다.
     */
    @Test
    @DisplayName("읽음 처리 Redis 장애 - fallback이 사용자 메타데이터를 DB에 직접 동기화한다")
    void markAsReadFallbackShouldSyncUserMetaToDatabase() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.markAsReadFallback(10L, 1L, 100L, openedAt, new RuntimeException("redis down"));

        verify(chatListRepository).syncUserMetaFromRedis(10L, 1L, 0, 100L, openedAt);
    }

    /**
     * 방 메타데이터를 Redis Hash와 Dirty Set에 함께 반영하는지 검증한다.
     */
    @Test
    @DisplayName("방 메타데이터 갱신 성공 - Redis Hash와 Dirty Set을 갱신한다")
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

    /**
     * 방 메타데이터 Redis fallback이 DB 직접 동기화 메서드로 위임되는지 검증한다.
     */
    @Test
    @DisplayName("방 메타데이터 Redis 장애 - fallback이 방 메타데이터를 DB에 직접 동기화한다")
    void updateRoomMetaFallbackShouldSyncRoomMetaToDatabase() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 11, 12, 0);

        service.updateRoomMetaFallback(10L, 100L, "preview", createdAt, new RuntimeException("redis down"));

        verify(chatRoomRepository).syncRoomMetaFromRedis(10L, 100L, "preview", createdAt);
    }

    /**
     * 사용자 Redis 메타 키가 이미 존재하면 캐시 워밍 없이 unreadCount만 증가시키는지 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 증가 성공 - 캐시가 존재하면 값 증가와 Dirty Set 등록만 수행한다")
    void incrementUnreadCountShouldIncrementExistingCache() {
        String key = "chat:room:10:user:1:meta";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        service.incrementUnreadCount(10L, 1L);

        verify(hashOperations).increment(key, "unreadCount", 1);
        verify(setOperations).add("chat:user:dirty", "10:1");
        verifyNoInteractions(chatListRepository);
    }

    /**
     * 캐시가 없는 상태에서 DB 값을 먼저 캐시에 적재한 뒤 증가시키는지 검증한다.
     * DB unreadCount를 잃고 0부터 증가하는 덮어쓰기 문제를 방지하는 테스트다.
     */
    @Test
    @DisplayName("안읽음 수 증가 캐시 미스 - DB unreadCount를 캐시 워밍한 뒤 1 증가시킨다")
    void incrementUnreadCountShouldWarmCacheBeforeIncrement() {
        String key = "chat:room:20:user:2:meta";
        ChatList chatList = chatList(5, null);
        when(redisTemplate.hasKey(key)).thenReturn(false);
        when(hashOperations.get(key, "unreadCount")).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.of(chatList));

        service.incrementUnreadCount(20L, 2L);

        verify(hashOperations).put(key, "unreadCount", "5");
        verify(hashOperations).increment(key, "unreadCount", 1);
        verify(setOperations).add("chat:user:dirty", "20:2");
    }

    /**
     * Redis 증가 fallback이 DB의 원자적 unread 증가 쿼리를 호출하는지 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 증가 Redis 장애 - fallback이 DB unreadCount 증가 쿼리를 호출한다")
    void incrementUnreadCountFallbackShouldIncreaseDatabaseValue() {
        service.incrementUnreadCountFallback(10L, 1L, new RuntimeException("redis down"));

        verify(chatListRepository).increaseUnreadCount(10L, 1L);
    }

    /**
     * unreadCount 캐시 히트 시 Repository를 조회하지 않고 정수값을 반환하는지 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 조회 성공 - Redis 캐시가 있으면 DB 조회 없이 값을 반환한다")
    void getUnreadCountShouldReturnCachedValue() {
        when(hashOperations.get("chat:room:10:user:1:meta", "unreadCount")).thenReturn("7");

        int result = service.getUnreadCount(10L, 1L);

        assertThat(result).isEqualTo(7);
        verifyNoInteractions(chatListRepository);
    }

    /**
     * 캐시 미스 시 DB 값을 반환하고 다음 조회를 위해 Redis에 저장하는지 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 조회 캐시 미스 - DB 값을 반환하고 Redis 캐시를 워밍한다")
    void getUnreadCountShouldLoadDatabaseValueAndWarmCache() {
        String key = "chat:room:10:user:1:meta";
        ChatList chatList = chatList(4, null);
        when(hashOperations.get(key, "unreadCount")).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 1L)).thenReturn(Optional.of(chatList));

        int result = service.getUnreadCount(10L, 1L);

        assertThat(result).isEqualTo(4);
        verify(hashOperations).put(key, "unreadCount", "4");
    }

    /**
     * DB에도 채팅 목록 row가 없는 신규 사용자에 대해 0을 반환하고 0을 캐시하는 경계값을 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 조회 경계값 - Redis와 DB 모두 값이 없으면 0을 반환하고 캐시한다")
    void getUnreadCountShouldUseZeroWhenDatabaseRowIsMissing() {
        String key = "chat:room:10:user:9:meta";
        when(hashOperations.get(key, "unreadCount")).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 9L)).thenReturn(Optional.empty());

        int result = service.getUnreadCount(10L, 9L);

        assertThat(result).isZero();
        verify(hashOperations).put(key, "unreadCount", "0");
    }

    /**
     * Redis Hash가 비어 있으면 방 메타데이터가 아직 없다고 판단해 null을 반환하는지 검증한다.
     */
    @Test
    @DisplayName("방 메타데이터 조회 경계값 - Redis Hash가 비어 있으면 null을 반환한다")
    void getRoomMetaShouldReturnNullWhenHashIsEmpty() {
        when(hashOperations.entries("chat:room:10:meta")).thenReturn(Map.of());

        ChatRoomMetaDto result = service.getRoomMeta(10L);

        assertThat(result).isNull();
    }

    /**
     * Redis Hash의 문자열 값을 타입에 맞게 변환해 DTO로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("방 메타데이터 조회 성공 - Redis Hash를 ChatRoomMetaDto로 변환한다")
    void getRoomMetaShouldConvertRedisHash() {
        LocalDateTime messageAt = LocalDateTime.of(2026, 7, 20, 16, 0);
        when(hashOperations.entries("chat:room:10:meta")).thenReturn(Map.of(
                "lastMessageId", "101",
                "lastMessagePreview", "새 메시지",
                "lastMessageAt", messageAt.toString()
        ));

        ChatRoomMetaDto result = service.getRoomMeta(10L);

        assertThat(result.lastMessageId()).isEqualTo(101L);
        assertThat(result.lastPreview()).isEqualTo("새 메시지");
        assertThat(result.lastMessageAt()).isEqualTo(messageAt);
    }

    /**
     * 숫자나 날짜 형식이 깨진 Redis 데이터는 예외를 외부에 노출하지 않고 null로 처리하는지 검증한다.
     */
    @Test
    @DisplayName("방 메타데이터 조회 예외 - 형식이 잘못된 Redis 값은 null로 안전하게 처리한다")
    void getRoomMetaShouldReturnNullWhenCachedValueIsMalformed() {
        when(hashOperations.entries("chat:room:10:meta")).thenReturn(Map.of(
                "lastMessageId", "not-number",
                "lastMessagePreview", "preview",
                "lastMessageAt", "not-date"
        ));

        assertThat(service.getRoomMeta(10L)).isNull();
    }

    /**
     * 멤버별 마지막 읽음 ID를 Redis 우선으로 조회하고 캐시가 없는 멤버만 DB에서 보완하는지 검증한다.
     */
    @Test
    @DisplayName("멤버 읽음 상태 조회 성공 - Redis 값과 DB fallback 값을 하나의 Map으로 반환한다")
    void getAllMembersLastReadIdShouldMergeCacheAndDatabaseValues() {
        String user1Key = "chat:room:10:user:1:meta";
        String user2Key = "chat:room:10:user:2:meta";
        when(hashOperations.get(user1Key, "lastReadMessageId")).thenReturn("100");
        when(hashOperations.get(user2Key, "lastReadMessageId")).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(chatList(3, 80L)));

        Map<Long, Long> result = service.getAllMembersLastReadId(10L, List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 100L).containsEntry(2L, 80L);
        verify(chatListRepository, never()).findByChatRoomIdAndUserId(10L, 1L);
        verify(hashOperations).put(user2Key, "lastReadMessageId", "80");
    }

    /**
     * 멤버의 DB row 또는 lastReadMessageId가 없으면 0으로 초기화하는 경계값을 검증한다.
     */
    @Test
    @DisplayName("멤버 읽음 상태 경계값 - Redis와 DB 값이 없으면 lastReadMessageId를 0으로 캐시한다")
    void getAllMembersLastReadIdShouldUseZeroForMissingMemberState() {
        String key = "chat:room:10:user:3:meta";
        when(hashOperations.get(key, "lastReadMessageId")).thenReturn(null);
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 3L)).thenReturn(Optional.empty());

        Map<Long, Long> result = service.getAllMembersLastReadId(10L, List.of(3L));

        assertThat(result).containsEntry(3L, 0L);
        verify(hashOperations).put(key, "lastReadMessageId", "0");
    }

    /**
     * Circuit Breaker fallback은 Redis 접근이나 캐시 워밍 없이 DB만 조회하는지 검증한다.
     */
    @Test
    @DisplayName("멤버 읽음 상태 Redis 장애 - fallback은 DB만 조회하고 Redis 쓰기를 수행하지 않는다")
    void getAllMembersLastReadIdFallbackShouldUseOnlyDatabase() {
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(chatList(1, 50L)));
        when(chatListRepository.findByChatRoomIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty());

        Map<Long, Long> result = service.getAllMembersLastReadIdFallback(
                10L, List.of(1L, 2L), new RuntimeException("redis down"));

        assertThat(result).containsEntry(1L, 50L).containsEntry(2L, 0L);
        verifyNoInteractions(hashOperations, setOperations);
    }

    /**
     * 멤버 목록이 비어 있으면 저장소 접근 없이 빈 Map을 반환하는 경계값을 검증한다.
     */
    @Test
    @DisplayName("멤버 읽음 상태 경계값 - 빈 멤버 목록이면 빈 Map을 반환한다")
    void getAllMembersLastReadIdShouldReturnEmptyMapForEmptyMembers() {
        Map<Long, Long> result = service.getAllMembersLastReadId(10L, List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(chatListRepository);
        verify(hashOperations, never()).get(anyString(), any());
    }

    private ChatList chatList(Integer unreadCount, Long lastReadMessageId) {
        ChatList chatList = ChatList.builder()
                .unreadCount(unreadCount)
                .build();
        ReflectionTestUtils.setField(chatList, "lastReadMessageId", lastReadMessageId);
        return chatList;
    }
}