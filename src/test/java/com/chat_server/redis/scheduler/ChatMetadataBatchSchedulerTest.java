package com.chat_server.redis.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatMetadataBatchSchedulerTest {

    private static final String USER_DIRTY_KEY = "chat:user:dirty";
    private static final String ROOM_DIRTY_KEY = "chat:room:dirty";
    private static final String USER_UPDATE_SQL =
            "UPDATE chat_list SET unread_count = ?, last_read_message_id = ?, last_opened_at = ? WHERE chat_room_id = ? AND user_id = ?";
    private static final String ROOM_UPDATE_SQL =
            "UPDATE chat_room SET last_message_id = ?, last_message_preview = ?, last_message_at = ? WHERE id = ?";

    private RedisTemplate<String, String> redisTemplate;
    private SetOperations<String, String> setOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private JdbcTemplate jdbcTemplate;
    private ChatMetadataBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        setOperations = mock(SetOperations.class);
        hashOperations = mock(HashOperations.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        scheduler = new ChatMetadataBatchScheduler(redisTemplate, jdbcTemplate);
    }

    /**
     * Dirty Set에 동기화 대상이 없을 때 DB와 Redis Hash를 조회하지 않는지 검증한다.
     * 불필요한 배치 쿼리와 Redis 접근이 발생하지 않아야 한다.
     */
    @Test
    @DisplayName("Redis 메타데이터 동기화 성공 - Dirty Set이 비어 있으면 DB 배치 갱신을 수행하지 않는다")
    void flushMetadataToDbShouldSkipAllWorkWhenDirtySetsAreEmpty() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(null);

        scheduler.flushMetadataToDb();

        verifyNoInteractions(hashOperations, jdbcTemplate);
        verify(setOperations, never()).remove(eq(USER_DIRTY_KEY), any());
        verify(setOperations, never()).remove(eq(ROOM_DIRTY_KEY), any());
    }

    /**
     * 사용자별 unreadCount, lastReadMessageId, lastOpenedAt을 JDBC batchUpdate 인자로 변환하는지 검증한다.
     * DB 반영에 성공한 Dirty Key만 Redis Set에서 제거되어야 한다.
     */
    @Test
    @DisplayName("사용자 메타데이터 동기화 성공 - Redis 값을 chat_list 배치 인자로 변환하고 성공한 Dirty Key를 제거한다")
    void flushMetadataToDbShouldFlushUserMetadataAndRemoveSuccessfulDirtyKey() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 7, 20, 10, 30);
        String userMetaKey = "chat:room:10:user:1:meta";

        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of("10:1", "invalid-format"));
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of());
        when(hashOperations.get(userMetaKey, "unreadCount")).thenReturn("3");
        when(hashOperations.get(userMetaKey, "lastReadMessageId")).thenReturn("99");
        when(hashOperations.get(userMetaKey, "lastOpenedAt")).thenReturn(openedAt.toString());

        scheduler.flushMetadataToDb();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(eq(USER_UPDATE_SQL), batchCaptor.capture());

        assertThat(batchCaptor.getValue()).hasSize(1);
        assertThat(batchCaptor.getValue().get(0))
                .containsExactly(3, 99L, openedAt, 10L, 1L);
        verify(setOperations).remove(USER_DIRTY_KEY, "10:1");
        verify(setOperations, never()).remove(USER_DIRTY_KEY, "invalid-format");
    }

    /**
     * lastReadMessageId가 문자열 "null"이고 lastOpenedAt이 비어 있는 복구 데이터를 검증한다.
     * null 값은 DB null로 변환하고, lastOpenedAt은 현재 시각으로 보정해야 한다.
     */
    @Test
    @DisplayName("사용자 메타데이터 validation - null 문자열과 누락된 lastOpenedAt을 안전한 값으로 보정한다")
    void flushMetadataToDbShouldNormalizeNullableUserMetadata() {
        String userMetaKey = "chat:room:20:user:2:meta";

        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of("20:2"));
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of());
        when(hashOperations.get(userMetaKey, "unreadCount")).thenReturn("0");
        when(hashOperations.get(userMetaKey, "lastReadMessageId")).thenReturn("null");
        when(hashOperations.get(userMetaKey, "lastOpenedAt")).thenReturn(null);

        LocalDateTime before = LocalDateTime.now();
        scheduler.flushMetadataToDb();
        LocalDateTime after = LocalDateTime.now();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(eq(USER_UPDATE_SQL), batchCaptor.capture());

        Object[] arguments = batchCaptor.getValue().get(0);
        assertThat(arguments[0]).isEqualTo(0);
        assertThat(arguments[1]).isNull();
        assertThat((LocalDateTime) arguments[2]).isBetween(before, after);
        assertThat(arguments[3]).isEqualTo(20L);
        assertThat(arguments[4]).isEqualTo(2L);
    }

    /**
     * unreadCount가 없는 사용자 메타데이터는 불완전한 데이터로 판단해 배치 대상에서 제외하는지 검증한다.
     * DB 갱신과 Dirty Key 제거가 모두 수행되지 않아야 한다.
     */
    @Test
    @DisplayName("사용자 메타데이터 validation 실패 - unreadCount가 없으면 DB 갱신과 Dirty Key 제거를 하지 않는다")
    void flushMetadataToDbShouldSkipUserMetadataWhenUnreadCountIsMissing() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of("30:3"));
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of());
        when(hashOperations.get("chat:room:30:user:3:meta", "unreadCount")).thenReturn(null);

        scheduler.flushMetadataToDb();

        verify(jdbcTemplate, never()).batchUpdate(eq(USER_UPDATE_SQL), anyList());
        verify(setOperations, never()).remove(USER_DIRTY_KEY, "30:3");
    }

    /**
     * 사용자 메타데이터 DB 배치 갱신이 실패했을 때 Dirty Key가 유지되는지 검증한다.
     * 다음 스케줄 실행에서 재시도할 수 있도록 Redis Set에서 제거하면 안 된다.
     */
    @Test
    @DisplayName("사용자 메타데이터 동기화 실패 - DB 예외가 발생하면 Dirty Key를 유지해 다음 배치에서 재시도한다")
    void flushMetadataToDbShouldKeepUserDirtyKeyWhenJdbcBatchUpdateFails() {
        String userMetaKey = "chat:room:40:user:4:meta";

        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of("40:4"));
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of());
        when(hashOperations.get(userMetaKey, "unreadCount")).thenReturn("2");
        when(hashOperations.get(userMetaKey, "lastReadMessageId")).thenReturn("100");
        when(hashOperations.get(userMetaKey, "lastOpenedAt")).thenReturn("2026-07-20T11:00:00");
        doThrow(new RuntimeException("database unavailable"))
                .when(jdbcTemplate).batchUpdate(eq(USER_UPDATE_SQL), anyList());

        scheduler.flushMetadataToDb();

        verify(setOperations, never()).remove(USER_DIRTY_KEY, "40:4");
    }

    /**
     * Dirty Set에 숫자로 변환할 수 없는 사용자 키가 들어온 예외 상황을 검증한다.
     * 스케줄러가 예외를 외부로 전파하지 않고 Dirty Key를 유지해야 한다.
     */
    @Test
    @DisplayName("사용자 메타데이터 예외 처리 - 숫자가 아닌 roomId가 포함되어도 스케줄러를 중단하지 않고 Dirty Key를 유지한다")
    void flushMetadataToDbShouldHandleMalformedUserDirtyKey() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of("room:user"));
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of());

        scheduler.flushMetadataToDb();

        verifyNoInteractions(jdbcTemplate);
        verify(setOperations, never()).remove(USER_DIRTY_KEY, "room:user");
    }

    /**
     * 채팅방의 마지막 메시지 ID, 미리보기, 생성 시각을 JDBC batchUpdate 인자로 변환하는지 검증한다.
     * DB 반영에 성공한 채팅방 Dirty Key만 제거되어야 한다.
     */
    @Test
    @DisplayName("채팅방 메타데이터 동기화 성공 - Redis Hash를 chat_room 배치 인자로 변환하고 Dirty Key를 제거한다")
    void flushMetadataToDbShouldFlushRoomMetadataAndRemoveSuccessfulDirtyKey() {
        LocalDateTime messageAt = LocalDateTime.of(2026, 7, 20, 12, 0);

        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of("50"));
        when(hashOperations.entries("chat:room:50:meta")).thenReturn(Map.of(
                "lastMessageId", "500",
                "lastMessagePreview", "새 메시지",
                "lastMessageAt", messageAt.toString()
        ));

        scheduler.flushMetadataToDb();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(eq(ROOM_UPDATE_SQL), batchCaptor.capture());

        assertThat(batchCaptor.getValue()).hasSize(1);
        assertThat(batchCaptor.getValue().get(0))
                .containsExactly(500L, "새 메시지", messageAt, 50L);
        verify(setOperations).remove(ROOM_DIRTY_KEY, "50");
    }

    /**
     * 채팅방 메타데이터에서 선택 값인 preview와 lastMessageAt이 누락된 경우를 검증한다.
     * preview는 빈 문자열, lastMessageAt은 현재 시각으로 보정되어야 한다.
     */
    @Test
    @DisplayName("채팅방 메타데이터 validation - 미리보기와 생성 시각이 없으면 기본값으로 보정한다")
    void flushMetadataToDbShouldNormalizeOptionalRoomMetadata() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of("60"));
        when(hashOperations.entries("chat:room:60:meta")).thenReturn(Map.of("lastMessageId", "600"));

        LocalDateTime before = LocalDateTime.now();
        scheduler.flushMetadataToDb();
        LocalDateTime after = LocalDateTime.now();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(eq(ROOM_UPDATE_SQL), batchCaptor.capture());

        Object[] arguments = batchCaptor.getValue().get(0);
        assertThat(arguments[0]).isEqualTo(600L);
        assertThat(arguments[1]).isEqualTo("");
        assertThat((LocalDateTime) arguments[2]).isBetween(before, after);
        assertThat(arguments[3]).isEqualTo(60L);
    }

    /**
     * lastMessageId가 없는 채팅방 메타데이터는 불완전한 데이터로 판단해 건너뛰는지 검증한다.
     * 잘못된 값으로 chat_room을 갱신하거나 Dirty Key를 제거하면 안 된다.
     */
    @Test
    @DisplayName("채팅방 메타데이터 validation 실패 - lastMessageId가 없으면 DB 갱신과 Dirty Key 제거를 하지 않는다")
    void flushMetadataToDbShouldSkipRoomMetadataWhenLastMessageIdIsMissing() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of("70"));
        when(hashOperations.entries("chat:room:70:meta")).thenReturn(Map.of("lastMessagePreview", "미완성 데이터"));

        scheduler.flushMetadataToDb();

        verify(jdbcTemplate, never()).batchUpdate(eq(ROOM_UPDATE_SQL), anyList());
        verify(setOperations, never()).remove(ROOM_DIRTY_KEY, "70");
    }

    /**
     * 채팅방 메타데이터 DB 배치 갱신 실패 시 Dirty Key가 유지되는지 검증한다.
     * 실패 데이터를 삭제하지 않아 다음 스케줄에서 다시 처리할 수 있어야 한다.
     */
    @Test
    @DisplayName("채팅방 메타데이터 동기화 실패 - DB 예외가 발생하면 Dirty Key를 유지한다")
    void flushMetadataToDbShouldKeepRoomDirtyKeyWhenJdbcBatchUpdateFails() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of("80"));
        when(hashOperations.entries("chat:room:80:meta")).thenReturn(Map.of(
                "lastMessageId", "800",
                "lastMessagePreview", "preview",
                "lastMessageAt", "2026-07-20T13:00:00"
        ));
        doThrow(new RuntimeException("database unavailable"))
                .when(jdbcTemplate).batchUpdate(eq(ROOM_UPDATE_SQL), anyList());

        scheduler.flushMetadataToDb();

        verify(setOperations, never()).remove(ROOM_DIRTY_KEY, "80");
    }

    /**
     * 숫자가 아닌 채팅방 Dirty Key가 들어온 예외 상황을 검증한다.
     * 스케줄러는 예외를 삼키고 해당 키를 유지해 운영 스케줄 전체가 중단되지 않아야 한다.
     */
    @Test
    @DisplayName("채팅방 메타데이터 예외 처리 - 숫자가 아닌 roomId가 있어도 스케줄러를 중단하지 않고 Dirty Key를 유지한다")
    void flushMetadataToDbShouldHandleMalformedRoomDirtyKey() {
        when(setOperations.members(USER_DIRTY_KEY)).thenReturn(Set.of());
        when(setOperations.members(ROOM_DIRTY_KEY)).thenReturn(Set.of("invalid-room"));

        scheduler.flushMetadataToDb();

        verifyNoInteractions(hashOperations, jdbcTemplate);
        verify(setOperations, never()).remove(ROOM_DIRTY_KEY, "invalid-room");
    }
}