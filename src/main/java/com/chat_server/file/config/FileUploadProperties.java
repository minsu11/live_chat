package com.chat_server.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {
    private String profileDir;
    private String chatImageDir;
    private String chatFileDir;
}
