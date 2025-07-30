package com.chat_server.error.properties;

import com.chat_server.error.enumulation.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "custom.error")
@Getter
@Setter
public class ErrorMessageProperties {

    private Map<String, String> messages;

    public String getMessages(ErrorCode errorCode) {
        return messages.getOrDefault(errorCode.name(),"알 수 없는 에러입니다.");
    }
}
