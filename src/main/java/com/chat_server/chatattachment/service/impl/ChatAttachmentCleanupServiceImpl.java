package com.chat_server.chatattachment.service.impl;

import com.chat_server.chatattachment.entity.ChatAttachment;
import com.chat_server.chatattachment.repository.ChatAttachmentRepository;
import com.chat_server.chatattachment.service.ChatAttachmentCleanupService;
import com.chat_server.file.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAttachmentCleanupServiceImpl implements ChatAttachmentCleanupService {

    private static final int ORPHAN_RETENTION_MINUTES = 30;

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final FileUploadProperties fileUploadProperties;

    @Override
    @Transactional
    public void cleanupOrphanAttachments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ORPHAN_RETENTION_MINUTES);

        while (true) {
            List<ChatAttachment> targets =
                    chatAttachmentRepository.findTop100ByChatMessageIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(cutoff);

            if (CollectionUtils.isEmpty(targets)) {
                return;
            }

            for (ChatAttachment attachment : targets) {
                deletePhysicalFileQuietly(attachment);
                chatAttachmentRepository.delete(attachment);
            }

            if (targets.size() < 100) {
                return;
            }
        }
    }

    private void deletePhysicalFileQuietly(ChatAttachment attachment) {
        try {
            Path rootPath = Paths.get(fileUploadProperties.getChatFileDir())
                    .toAbsolutePath()
                    .normalize();

            Path targetPath = rootPath.resolve(attachment.getFileUrl()).normalize();

            if (!targetPath.startsWith(rootPath)) {
                log.warn("orphan attachment cleanup skipped. invalid path. attachmentId={}, fileUrl={}",
                        attachment.getId(), attachment.getFileUrl());
                return;
            }

            Files.deleteIfExists(targetPath);
        } catch (Exception e) {
            log.warn("orphan attachment file delete failed. attachmentId={}, fileUrl={}",
                    attachment.getId(), attachment.getFileUrl(), e);
        }
    }
}
