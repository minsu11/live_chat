package com.chat_server.redis.propertie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *packageName    : com.chat_server.common.redis.config
 * fileName       : RedisConfig
 * author         : parkminsu
 * date           : 25. 3. 12.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 12.        parkminsu       최초 생성
 *
 */
@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "custom.redis")
public class RedisProperties {
    private String host;
    private int port;
    private int timeout;
    private int cacheTimeout;
}
