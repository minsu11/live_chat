package com.chat_server.chatattachment.dto.response;

public record ChatAttachmentUploadResponse(
        Long attachmentId,
        String fileName,
        String contentType,
        Long fileSize
) {
}
