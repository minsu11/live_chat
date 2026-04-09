package com.chat_server.file.dto.response;

public record ChatImageUploadResponse(
        String fileUrl,
        String originalFileName,
        String contentType,
        long fileSize
) {
}
