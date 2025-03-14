package com.chat_server.common.redis.repository;

/**
 * packageName    : com.chat_server.common.redis.repository
 * fileName       : RedisRepository
 * author         : parkminsu
 * date           : 25. 3. 12.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 12.        parkminsu       최초 생성
 */
public interface RedisRepository {
    void saveData(String key, String value, long ttl);
}
