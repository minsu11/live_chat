package com.chat_server.file.service.impl;

import com.chat_server.file.dto.response.ChatFileUploadResponse;
import com.chat_server.file.dto.response.ChatImageUploadResponse;
import com.chat_server.file.service.ChatFileFacadeService;
import com.chat_server.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFileFacadeServiceImpl implements ChatFileFacadeService {
    private final FileService fileService;

    @Override
    public ChatImageUploadResponse uploadChatImage(Long userId, MultipartFile file) {
        log.debug("upload chat image start");
        String fileUrl = fileService.saveChatImage(userId, file);

        return new ChatImageUploadResponse(
                fileUrl,
                Objects.requireNonNullElse(file.getOriginalFilename(), ""),
                file.getContentType(),
                file.getSize()
        );
    }

    @Override
    public ChatFileUploadResponse uploadChatFile(Long userId, MultipartFile file) {
        String fileUrl = fileService.saveChatFile(userId, file);
        return new ChatFileUploadResponse(
                fileUrl,
                Objects.requireNonNullElse(file.getOriginalFilename(), ""),
                file.getContentType(),
                file.getSize()
        );
    }
}
