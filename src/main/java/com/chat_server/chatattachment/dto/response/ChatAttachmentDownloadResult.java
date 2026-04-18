package com.chat_server.chatattachment.dto.response;

import org.springframework.core.io.Resource;

public record ChatAttachmentDownloadResult(
        Resource resource,
        String fileName,
        String contentType,
        Long contentLength
) {
}
