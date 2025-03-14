package com.chat_server.common.config;

import com.chat_server.common.redis.config.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.RegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

/**
 * packageName    : com.chat_server.common.config
 * fileName       : CommonConfig
 * author         : parkminsu
 * date           : 25. 3. 4.
 * description    : 공통적으로 등록할 Bean 관리 용 Configuration
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 4.        parkminsu       최초 생성
 */
@Configuration
@RequiredArgsConstructor
public class CommonConfig {
    private final RedisConfig redisConfig;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    // todo reids time connect 등 여러 설정들을 만져야함

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());

        RedisConnectionFactory factory = new LettuceConnectionFactory(config);
        return factory;
    }

    @Bean
    public RedisTemplate<String,Object> restTemplate()    {
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        ObjectMapper objectMapper = objectMapper();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        // value object type -> json 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        // spring에서 bean 등록 후 자동으로 호출이 되긴함
        template.afterPropertiesSet();
        return template;
    }
}
