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

        /**
         * ErrorCode enum 기반으로 사용자 노출 메시지를 조회한다.
         *
         * <p>기능:
         * <ul>
         *   <li>ErrorCode가 null이면 NOT_DEFINE 메시지로 fallback 한다.</li>
         *   <li>ErrorCode가 존재하면 enum name을 문자열 키로 변환해 조회한다.</li>
         * </ul>
         *
         * @param errorCode 메시지를 조회할 에러 코드(enum)
         * @return 에러 코드에 대응하는 메시지(없으면 NOT_DEFINE 또는 기본 문구)
         */
        public String getMessage(ErrorCode errorCode) {
            if (errorCode == null) {
                return getMessage("NOT_DEFINE");
            }
            return getMessage(errorCode.name());
        }

        /**
         * 문자열 코드 기반으로 사용자 노출 메시지를 조회한다.
         *
         * <p>기능:
         * <ul>
         *   <li>messages 설정이 비어 있으면 하드코딩 기본 문구를 반환한다.</li>
         *   <li>errorCode가 null/blank이면 NOT_DEFINE 메시지를 반환한다.</li>
         *   <li>해당 키가 없으면 NOT_DEFINE 메시지로 fallback 한다.</li>
         * </ul>
         *
         * @param errorCode yml messages 맵의 키 값(예: USER_NOT_FOUND)
         * @return 키에 매핑된 메시지 또는 fallback 메시지
         */
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
