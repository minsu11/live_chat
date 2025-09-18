package com.chat_server.common.propertis;

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
    }

    @Getter
    @Setter
    public static class Api {
        private String prefix;
    }
}

