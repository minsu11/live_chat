package com.chat_server.chatattachment.service.impl;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatattachment.entity.ChatAttachment;
import com.chat_server.chatattachment.repository.ChatAttachmentRepository;
import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.file.config.FileUploadProperties;
import com.chat_server.file.exception.FileValidationException;
import com.chat_server.user.entity.User;
import com.chat_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final FileUploadProperties fileUploadProperties;
    private final CustomProperties customProperties;

    @Override
    @Transactional
    public ChatAttachmentUploadResponse upload(Long roomId, Long userId, MultipartFile file) {
        validateActiveMember(roomId, userId);
        validateUploadFile(file);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "채팅방이 존재하지 않습니다."));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자가 존재하지 않습니다."));

        if (!StringUtils.hasText(fileUploadProperties.getChatFileDir())) {
            throw new FileValidationException(
                    ErrorCode.NOT_DEFINE,
                    "채팅 파일 저장 경로 설정이 비어 있습니다."
            );
        }

        Path chatFileRootPath = Paths.get(fileUploadProperties.getChatFileDir())
                .toAbsolutePath()
                .normalize();

        String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "file";

        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;

        LocalDate today = LocalDate.now();
        String relativePath = Paths.get(
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                storedFileName
        ).toString().replace("\\", "/");

        Path targetPath = chatFileRootPath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(chatFileRootPath)) {
            throw new ResponseStatusException(BAD_REQUEST, "잘못된 파일 경로입니다.");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.", e);
        }

        ChatAttachment attachment = chatAttachmentRepository.save(
                ChatAttachment.builder()
                        .chatMessage(null)
                        .room(room)
                        .uploader(uploader)
                        .fileUrl(relativePath) // 내부 상대경로 저장
                        .originalFileName(originalFileName)
                        .storedFileName(storedFileName)
                        .contentType(file.getContentType())
                        .fileSize(file.getSize())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return new ChatAttachmentUploadResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getFileSize()
        );
    }

    @Override
    @Transactional
    public void connectMessage(Long attachmentId, ChatMessage chatMessage, Long userId) {
        if (attachmentId == null) {
            return;
        }

        ChatAttachment attachment = chatAttachmentRepository
                .findByIdAndUploader_IdAndChatMessageIsNull(attachmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "연결 가능한 첨부파일이 없습니다."));

        Long messageRoomId = chatMessage.getChatRoom().getId();
        Long messageSenderId = chatMessage.getSender().getId();

        if (!attachment.getRoom().getId().equals(messageRoomId)) {
            throw new ResponseStatusException(FORBIDDEN, "다른 채팅방의 첨부파일은 연결할 수 없습니다.");
        }

        if (!attachment.getUploader().getId().equals(messageSenderId)) {
            throw new ResponseStatusException(FORBIDDEN, "본인이 업로드한 첨부파일만 연결할 수 있습니다.");
        }

        attachment.connectMessage(chatMessage);
    }

    @Override
    public ChatAttachmentDownloadResult download(Long attachmentId, Long userId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "첨부파일이 존재하지 않습니다."));

        validateActiveMember(attachment.getRoom().getId(), userId);

        if (attachment.getChatMessage() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "아직 메시지에 연결되지 않은 파일입니다.");
        }

        String chatFileDir = fileUploadProperties.getChatFileDir();

        if (!StringUtils.hasText(chatFileDir)) {
            throw new FileValidationException(
                    ErrorCode.NOT_DEFINE,
                    "채팅 파일 저장 경로 설정이 비어 있습니다."
            );
        }

        Path chatFileRootPath = Paths.get(chatFileDir)
                .toAbsolutePath()
                .normalize();

        Path targetPath = chatFileRootPath.resolve(attachment.getFileUrl()).normalize();

        if (!targetPath.startsWith(chatFileRootPath)) {
            throw new ResponseStatusException(BAD_REQUEST, "잘못된 파일 접근입니다.");
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResponseStatusException(NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        try {
            Resource resource = new UrlResource(targetPath.toUri());

            return new ChatAttachmentDownloadResult(
                    resource,
                    attachment.getOriginalFileName(),
                    detectContentType(targetPath, attachment.getContentType()),
                    Files.size(targetPath)
            );
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "파일 다운로드에 실패했습니다.", e);
        }
    }

    private void validateActiveMember(Long roomId, Long userId) {
        boolean isMember = chatRoomMemberRepository.existsActiveMember(roomId, userId);
        if (!isMember) {
            throw new ResponseStatusException(FORBIDDEN, "채팅방 멤버만 파일에 접근할 수 있습니다.");
        }
    }

    private void validateUploadFile(MultipartFile file) {
        DataSize maxFileSize = fileUploadProperties.getChatFileMaxSize();
        if (file == null || file.isEmpty()) {
            throw new FileValidationException(
                    ErrorCode.INVALID_INPUT,
                    "빈 파일은 전송할 수 없습니다."
            );
        }

        if (maxFileSize == null) {
            throw new FileValidationException(
                    ErrorCode.NOT_DEFINE,
                    "채팅 파일 최대 크기 설정이 비어 있습니다."
            );
        }

        // multipart max-file-size보다 작은 범위에서만 여기 도달함
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new FileValidationException(
                    ErrorCode.FILE_SIZE_EXCEEDED,
                    "파일 크기가 제한을 초과했습니다."
            );
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileValidationException(
                    ErrorCode.INVALID_INPUT,
                    "파일명이 올바르지 않습니다."
            );
        }

        String extension = extractExtension(originalFileName)
                .replace(".", "")
                .toLowerCase();
        if (extension.isBlank()) {
            throw new FileValidationException(
                    ErrorCode.INVALID_INPUT,
                    "파일 확장자가 없습니다."
            );
        }

        Set<String> allowedExtensions = fileUploadProperties.getChatFileAllowedExtensions();
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            throw new FileValidationException(
                    ErrorCode.NOT_DEFINE,
                    "허용 파일 확장자 설정이 비어 있습니다."
            );
        }

        boolean allowed = allowedExtensions.stream()
                .filter(StringUtils::hasText)
                .map(ext -> ext.trim().toLowerCase())
                .anyMatch(ext -> ext.equals(extension));

        if (!allowed) {
            throw new FileValidationException(
                    ErrorCode.INVALID_INPUT,
                    "허용되지 않는 파일 형식입니다."
            );
        }
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index);
    }

    private String detectContentType(Path path, String fallback) {
        try {
            String detected = Files.probeContentType(path);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (Exception ignored) {
        }

        return StringUtils.hasText(fallback)
                ? fallback
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}