package com.chat_server.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String saveProfileImage(Long userId, MultipartFile file);

    String saveChatImage(Long userId, MultipartFile file);

    String saveChatFile(Long userId, MultipartFile file);
}
