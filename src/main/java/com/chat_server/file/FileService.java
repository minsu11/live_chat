package com.chat_server.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String saveProfileImage(Long userId, MultipartFile file);
}
