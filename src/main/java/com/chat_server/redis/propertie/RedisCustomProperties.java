package com.chat_server.redis.propertie;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 애플리케이션 커스텀 설정
 * 접속 정보(host, port, password)는 spring.data.redis 를 사용한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.redis")
public class RedisCustomProperties {

    /**
     * Redis command timeout (milliseconds)
     */
    private long timeout = 3000L;

    /**
     * Cache TTL (minutes)
     */
    private long cacheTimeout = 10L;
}