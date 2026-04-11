package com.chat_server.file.dto.response;

public record ChatFileUploadResponse(
        String fileUrl,
        String originalFileName,
        String contentType,
        long fileSize
) {
}
