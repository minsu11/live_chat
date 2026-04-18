package com.chat_server.chatattachment.dto.response;

public record ChatAttachmentMessagePayload(
        Long attachmentId,
        String fileName,
        String contentType,
        Long fileSize
) {
}
