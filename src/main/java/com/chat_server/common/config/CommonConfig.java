package com.chat_server.common.config;

import com.chat_server.common.redis.config.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.resource.DefaultClientResources;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.RegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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
    // 해당 설정들은 명시적으로 리소스 관리를 해주기 위한 것,
    // 애플리케이션 종료 시 Shutdown
    private final DefaultClientResources clientResources = DefaultClientResources.create();

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    // todo reids time connect 등 여러 설정들을 만져야함
    // redis 비밀번호 설정도 추가해야함
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());

        // time out 5초
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofMillis(100))
                .clientResources(clientResources)
                .build();

        return new LettuceConnectionFactory(config);
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
