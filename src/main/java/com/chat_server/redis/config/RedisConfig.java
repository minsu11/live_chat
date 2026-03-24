package com.chat_server.redis.config;

import com.chat_server.redis.propertie.RedisCustomProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisCustomProperties.class)
public class RedisConfig {

    /**
     * spring.data.redis.* 를 Spring Boot가 바인딩해주는 기본 프로퍼티
     */
    private final RedisProperties redisProperties;

    /**
     * custom.redis.* 를 바인딩하는 네 커스텀 프로퍼티
     */
    private final RedisCustomProperties redisCustomProperties;

    private final DefaultClientResources clientResources = DefaultClientResources.create();


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setDatabase(redisProperties.getDatabase());


        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisCustomProperties.getTimeout()))
                .shutdownTimeout(Duration.ofMillis(100))
                .clientResources(clientResources)
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}