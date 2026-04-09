package com.chat_server.file.service;

import com.chat_server.file.dto.response.ChatImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChatFileFacadeService {
    ChatImageUploadResponse uploadChatImage(Long userId, MultipartFile file);
}
