package com.chat_server.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 명시적으로 ws-chat 경로를 리소스로 처리하지 않음
        registry.addResourceHandler("/ws-chat/**").addResourceLocations("classpath:/").resourceChain(false);
    }
}
