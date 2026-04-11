package com.chat_server.file.service.impl;

import com.chat_server.file.service.FileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import com.chat_server.file.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileUploadProperties fileUploadProperties;
    @Override
    public String saveProfileImage(Long userId, MultipartFile file) {
        validateFile(file);
        validateImage(file);
        return save(userId, file, fileUploadProperties.getProfileDir(), "/uploads/profile/");
    }

    @Override
    public String saveChatImage(Long userId, MultipartFile file) {
        validateFile(file);
        validateImage(file);
        return save(userId, file, fileUploadProperties.getChatImageDir(), "/uploads/chat/images/");
    }

    @Override
    public String saveChatFile(Long userId, MultipartFile file) {
        validateFile(file);
        validateChatFile(file);
        return save(userId, file, fileUploadProperties.getChatFileDir(), "/uploads/chat/files/");
    }

    private String save(Long userId, MultipartFile file, String dir, String urlPrefix) {
        try {
            Path uploadDir = Path.of(dir);
            Files.createDirectories(uploadDir);

            String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
            String filename = "u" + userId + "_" + UUID.randomUUID() + ext;

            Path savePath = uploadDir.resolve(filename);
            file.transferTo(savePath);

            log.info("파일 저장 완료. userId={}, path={}", userId, savePath);
            return urlPrefix + filename;
        } catch (IOException e) {
            log.error("파일 저장 실패. userId={}", userId, e);
            throw new RuntimeException("파일 저장에 실패했습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }
    private void validateChatFile(MultipartFile file) {
        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase();

        boolean allowed =
                fileName.endsWith(".pdf") ||
                        fileName.endsWith(".doc") ||
                        fileName.endsWith(".docx") ||
                        fileName.endsWith(".xls") ||
                        fileName.endsWith(".xlsx") ||
                        fileName.endsWith(".txt") ||
                        fileName.endsWith(".zip");

        if (!allowed) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }

        long maxSize = 20 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 최대 크기를 초과했습니다.");
        }
    }
    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf(".");
        return idx > 0 ? fileName.substring(idx) : "";
    }
}
