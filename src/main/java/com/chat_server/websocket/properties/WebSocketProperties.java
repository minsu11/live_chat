package com.chat_server.websocket.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(value = "custom.web-socket")
public class WebSocketProperties {
    private String subPrefix ;
    private String pubPrefix;
    private String endPoint;
    private Chat chat;

    @Getter
    @Setter
    public static class Chat{
        private String messagePath;
        private String roomPath;
    }
}
