package com.chat_server.common.redis.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Save.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the ttl
     */
    void save(String key, Object value, long ttl);

    /**
     * Find by key object.
     *
     * @param key the key
     * @return the object
     */
    <T> Optional<T> findByKey(String key, Class<T> clazz);


    /**
     * Exists boolean.
     *
     * @param key the key
     * @return the boolean
     */
    boolean exists(String key);

    /**
     * Delete by key.
     *
     * @param key the key
     */
    void deleteByKey(String key);

    /**
     * Push to list.
     *
     * @param key   the key
     * @param value the value
     */
    void pushToList(String key, Object value);

    /**
     * Pop from list object.
     *
     * @param key the key
     * @return the object
     */
    <T> Optional<T> popFromList(String key, Class<T> clazz);

    /**
     * Gets list.
     *
     * @param key   the key
     * @param start the start
     * @param end   the end
     * @return the list
     */
    <T> Optional<List<T>> getList(String key, int start, int end, Class<T> tClass);

    /**
     * Put to hash.
     *
     * @param hashKey the hash key
     * @param field   the field
     * @param value   the value
     */
    void putToHash(String hashKey, String field, Object value);

    /**
     * Gets from hash.
     *
     * @param hashKey the hash key
     * @param field   the field
     * @return the from hash
     */
    <T> Optional<T> getFromHash(String hashKey, String field, Class<T> tClass);

    /**
     * Increment long.
     *
     * @param key   the key
     * @param delta the delta
     * @return the long
     */
    long increment(String key, long delta);

    /**
     * Decrement long.
     *
     * @param key   the key
     * @param delta the delta
     * @return the long
     */
    long decrement(String key, long delta);

    /**
     * Sets expiration.
     *
     * @param key     the key
     * @param seconds the seconds
     */
    void setExpiration(String key, long seconds);

    /**
     * Gets expiration.
     *
     * @param key the key
     * @return the expiration
     */
    long getExpiration(String key);

    /**
     * Find keys by pattern set.
     *
     * @param pattern the pattern
     * @return the set
     */
    Optional<Set<String>> findKeysByPattern(String pattern);

    /**
     * Delete keys by pattern.
     *
     * @param pattern the pattern
     */
    void deleteKeysByPattern(String pattern);

}
