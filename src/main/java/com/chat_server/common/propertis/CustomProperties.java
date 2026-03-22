package com.chat_server.common.propertis;

import com.chat_server.error.enumulation.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "custom")
public class CustomProperties {

    private Auth auth;
    private Error error;
    private Api api;

    @Getter
    @Setter
    public static class Auth {
        private Header header;

        @Getter
        @Setter
        public static class Header {
            private String tokenExpiration;
        }
    }

    @Getter
    @Setter
    public static class Error {
        private Map<String, String> messages;

        public String getMessage(ErrorCode errorCode) {
            if (errorCode == null) {
                return getMessage("NOT_DEFINE");
            }
            return getMessage(errorCode.name());
        }

        public String getMessage(String errorCode) {
            if (messages == null || messages.isEmpty()) {
                return "알 수 없는 에러 입니다.";
            }

            if (errorCode == null || errorCode.isBlank()) {
                return messages.getOrDefault("NOT_DEFINE", "알 수 없는 에러 입니다.");
            }

            return messages.getOrDefault(
                    errorCode,
                    messages.getOrDefault("NOT_DEFINE", "알 수 없는 에러 입니다.")
            );
        }
    }

    @Getter
    @Setter
    public static class Api {
        private Common common;
        private User user;
        private Friend friend;
        private Search search;
        private ChatRoom chatRoom;

        @Getter
        @Setter
        public static class Common {
            private String prefix;
        }

        @Getter
        @Setter
        public static class User {
            private String prefix;
        }

        @Getter
        @Setter
        public static class Friend {
            private String prefix;
        }

        @Getter
        @Setter
        public static class Search {
            private String prefix;
        }

        @Getter
        @Setter
        public static class ChatRoom {
            private String prefix;
        }
    }
}