package com.chat_server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class CommonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
