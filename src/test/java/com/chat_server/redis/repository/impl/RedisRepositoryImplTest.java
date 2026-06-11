package com.chat_server.redis.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RedisRepositoryImplTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ListOperations<String, Object> listOperations;
    private RedisRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        repository = new RedisRepositoryImpl(redisTemplate);
    }

    @Test
    @DisplayName("Redis 저장 성공 시 key, value, ttl을 초 단위로 저장한다")
    void saveShouldStoreValueWithTtlInSeconds() {
        repository.save("token:1", "value", 60L);

        verify(valueOperations).set("token:1", "value", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Redis 저장 실패 시 key/value/ttl이 유효하지 않으면 예외를 발생시킨다")
    void saveShouldRejectInvalidArguments() {
        assertThatThrownBy(() -> repository.save(null, "value", 60L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> repository.save("", "value", 60L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> repository.save("key", null, 60L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> repository.save("key", "value", 0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.save("key", "value", 1_000_000L)).isInstanceOf(IllegalArgumentException.class);

        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Redis 조회 성공 시 저장된 값을 요청 타입으로 반환한다")
    void findByKeyShouldReturnTypedValue() {
        when(valueOperations.get("token:1")).thenReturn("value");

        Optional<String> result = repository.findByKey("token:1", String.class);

        assertThat(result).contains("value");
    }

    @Test
    @DisplayName("Redis 조회 성공 시 값이 없으면 Optional.empty를 반환한다")
    void findByKeyShouldReturnEmptyWhenValueIsMissing() {
        when(valueOperations.get("missing")).thenReturn(null);

        assertThat(repository.findByKey("missing", String.class)).isEmpty();
    }

    @Test
    @DisplayName("Redis key 존재 여부와 삭제 요청을 RedisTemplate에 위임한다")
    void existsAndDeleteShouldDelegateToRedisTemplate() {
        when(redisTemplate.hasKey("token:1")).thenReturn(true);

        assertThat(repository.exists("token:1")).isTrue();
        repository.deleteByKey("token:1");

        verify(redisTemplate).delete("token:1");
    }

    @Test
    @DisplayName("Redis 리스트 push/pop 성공 시 ListOperations를 사용한다")
    void pushAndPopListShouldUseListOperations() {
        when(listOperations.leftPop("queue")).thenReturn("item");

        repository.pushToList("queue", "item");
        Optional<String> popped = repository.popFromList("queue", String.class);

        verify(listOperations).rightPush("queue", "item");
        assertThat(popped).contains("item");
    }
}
