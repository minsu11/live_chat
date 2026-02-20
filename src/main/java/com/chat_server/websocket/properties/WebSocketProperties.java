package com.chat_server.websocket.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(value = "custom.web-socket")
public class WebSocketCommonProperties {
    private Sub sub;

    @Getter
    @Setter
    class Sub{
        private String api;
    }
}
