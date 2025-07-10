package com.chat_server.redis.repository.impl;

import com.chat_server.redis.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String key, Object value, long ttl) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        checkNull(value, "value");

        if (ttl <= 0 || ttl > 999999) {
            throw new IllegalArgumentException("ttl must be greater than 0");
        }

        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
    }

    @Override
    public <T> Optional<T> findByKey(String key, Class<T> clazz) {
        checkNull(key, "key");
        checkEmpty(key, "key");
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value).map(clazz::cast);
    }

    @Override
    public boolean exists(String key) {
        checkNull(key, "key");
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
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
        Object value = redisTemplate.opsForList().leftPop(key);
        return Optional.ofNullable(value).map(val -> {
            try{
                return clazz.cast(val);
            }catch (ClassCastException e){
                log.warn("ClassCastException", e);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    @Override
    public <T> Optional<List<T>> getList(String key, int start, int end, Class<T> clazz) {
        if (start > end) {
            throw new IllegalArgumentException("start must be less than end");
        }
        List<Object> values = redisTemplate.opsForList().range(key, start, end);
        if (values == null || values.isEmpty()) return Optional.empty();
        List<T> result = values.stream().map(clazz::cast).toList();
        return Optional.of(result);
    }

    @Override
    public void putToHash(String hashKey, String field, Object value) {
        checkNull(hashKey, "hashKey");
        checkEmpty(hashKey, "hashKey");
        checkNull(field, "field");
        checkEmpty(field, "field");
        checkNull(value, "value");

        redisTemplate.opsForHash().put(hashKey, field, value);
    }

    @Override
    public <T> Optional<T> getFromHash(String hashKey, String field, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(hashKey, field);
        return Optional.ofNullable(value).map(clazz::cast);
    }

    @Override
    public long increment(String key, long delta) {
        checkNull(key, "key");
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public long decrement(String key, long delta) {
        checkNull(key, "key");
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public void setExpiration(String key, long seconds) {
        checkNull(key, "key");
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    @Override
    public long getExpiration(String key) {
        checkNull(key, "key");
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -1;
    }

    @Override
    public Optional<Set<String>> findKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return Optional.ofNullable(keys);
    }

    @Override
    public void deleteKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
