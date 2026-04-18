package com.chat_server.file.dto.response;

import org.springframework.core.io.Resource;

public record ChatFileDownloadResult(
        Resource resource,
        String fileName,
        String contentType,
        Long contentLength
) {
}
