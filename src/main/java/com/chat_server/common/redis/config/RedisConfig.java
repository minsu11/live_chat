package com.chat_server.common.redis.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
@ConfigurationProperties(prefix = "redis")
public class RedisConfig {
    private String host;
    private int port;
    private int timeout;
}
