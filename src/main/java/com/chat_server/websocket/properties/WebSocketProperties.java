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
    private Event event;

    @Getter
    @Setter
    public static class Chat{
        private String messagePath;
        private String roomPath;
        private String summaryEventPath;
        private String chatList;
        private String chatNotification;
    }

    @Getter
    @Setter
    public static class Event{
        private String roomSummaryUpdated;
        private String messageRead;
        private String chatNotification;
        private String chatListUpsert;
    }
}
