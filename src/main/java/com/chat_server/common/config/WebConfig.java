package com.chat_server.common.config;

import com.chat_server.file.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final FileUploadProperties fileUploadProperties;
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 명시적으로 ws-chat 경로를 리소스로 처리하지 않음
        registry.addResourceHandler("/ws-chat/**").addResourceLocations("classpath:/").resourceChain(false);
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations("file:" + ensureTrailingSlash(fileUploadProperties.getProfileDir()));

        registry.addResourceHandler("/uploads/chat/images/**")
                .addResourceLocations("file:" + ensureTrailingSlash(fileUploadProperties.getChatImageDir()));

        registry.addResourceHandler("/uploads/chat/files/**")
                .addResourceLocations("file:" + ensureTrailingSlash(fileUploadProperties.getChatFileDir()));
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") || path.endsWith("\\") ? path : path + "/";
    }
}
