package com.chat_server.common.propertis;

import com.chat_server.error.enumulation.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
    @Slf4j
    public static class Error {
        private Map<String, String> messages;

        public String getMessages(ErrorCode errorCode){
            log.error(errorCode.toString());
            if(messages == null){
                return "error code 없음";
            }
            return messages.getOrDefault(errorCode.name(), "알 수 없는 에러 입니다.");
        }
    }

    @Getter
    @Setter
    public static class Api {
        @Getter
        @Setter
        public static class User{
            private String prefix;

        }
    }
}

