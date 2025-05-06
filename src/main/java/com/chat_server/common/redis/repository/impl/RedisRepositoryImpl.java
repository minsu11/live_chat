package com.chat_server.common.redis.repository.impl;

import com.chat_server.common.redis.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

import static com.chat_server.common.validation.Validation.checkEmpty;
import static com.chat_server.common.validation.Validation.checkNull;

/**
 * packageName    : com.chat_server.common.redis.repository.impl
 * fileName       : RedisRepositoryImpl
 * author         : parkminsu
 * date           : 25. 3. 12.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 12.        parkminsu       최초 생성
 */
@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String key, Object value, long ttl) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        checkNull(value, "value");

        // ttl 시간 설정은 추후 설정
        if (ttl <= 0 || ttl > 999999) {
            throw new IllegalArgumentException("ttl must be greater than 0");
        }

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public <T> Optional<T> findByKey(String key, Class<T> clazz) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        Object value = redisTemplate.opsForValue().get(key);
        checkNull(value, "value");

        return Optional.ofNullable(value).map(clazz::cast);
    }

    @Override
    public boolean exists(String key) {
        checkNull(key, "key");
        Object value = redisTemplate.opsForValue().get(key);

        return value != null;
    }

    @Override
    public void deleteByKey(String key) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        redisTemplate.delete(key);
    }

    @Override
    public void pushToList(String key, Object value) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        checkNull(value, "value");

        redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public <T> Optional<T> popFromList(String key, Class<T> clazz) {
        Map<String, Integer> map = new HashMap<>();
        Set<Integer> s = new HashSet<>();
        return null;
    }

    @Override
    public <T> Optional<List<T>> getList(String key, int start, int end, Class<T> clazz) {
        return Optional.empty();
    }

    @Override
    public void putToHash(String hashKey, String field, Object value) {

    }

    @Override
    public <T> Optional<T> getFromHash(String hashKey, String field, Class<T> clazz) {
        return null;
    }

    @Override
    public long increment(String key, long delta) {
        return 0;
    }

    @Override
    public long decrement(String key, long delta) {
        return 0;
    }

    @Override
    public void setExpiration(String key, long seconds) {

    }

    @Override
    public long getExpiration(String key) {
        return 0;
    }

    @Override
    public Optional<Set<String>> findKeysByPattern(String pattern) {
        return Optional.empty();
    }

    @Override
    public void deleteKeysByPattern(String pattern) {

    }
}
