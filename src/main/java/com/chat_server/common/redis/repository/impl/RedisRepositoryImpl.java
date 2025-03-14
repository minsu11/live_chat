package com.chat_server.common.redis.repository.impl;

import com.chat_server.common.redis.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

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
    public void saveData(String key, String value, long ttl) {

    }
}
