package com.chat_server.chatattachment.service.impl;

import com.chat_server.chatattachment.entity.ChatAttachment;
import com.chat_server.chatattachment.repository.ChatAttachmentRepository;
import com.chat_server.file.config.FileUploadProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatAttachmentCleanupServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("orphan attachment 정리 성공 시 오래된 미연결 첨부파일만 삭제하고 metadata를 삭제한다")
    void cleanupOrphanAttachmentsShouldDeleteOldOrphanFileAndMetadata() throws Exception {
        ChatAttachmentRepository repository = mock(ChatAttachmentRepository.class);
        FileUploadProperties properties = new FileUploadProperties();
        properties.setChatFileDir(tempDir.toString());
        ChatAttachmentCleanupServiceImpl service = new ChatAttachmentCleanupServiceImpl(repository, properties);
        Path file = tempDir.resolve("2026/06/11/orphan.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "orphan");
        ChatAttachment orphan = attachment(1L, "2026/06/11/orphan.txt");
        when(repository.findTop100ByChatMessageIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(orphan))
                .thenReturn(List.of());

        service.cleanupOrphanAttachments();

        assertThat(Files.exists(file)).isFalse();
        verify(repository).delete(orphan);
    }

    @Test
    @DisplayName("orphan attachment 정리 성공 시 삭제 대상이 없으면 metadata 삭제를 호출하지 않는다")
    void cleanupOrphanAttachmentsShouldDoNothingWhenNoTargetsExist() {
        ChatAttachmentRepository repository = mock(ChatAttachmentRepository.class);
        FileUploadProperties properties = new FileUploadProperties();
        properties.setChatFileDir(tempDir.toString());
        ChatAttachmentCleanupServiceImpl service = new ChatAttachmentCleanupServiceImpl(repository, properties);
        when(repository.findTop100ByChatMessageIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(any())).thenReturn(List.of());

        service.cleanupOrphanAttachments();

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("orphan attachment 정리 중 파일 삭제가 실패해도 metadata 삭제는 계속 수행한다")
    void cleanupOrphanAttachmentsShouldContinueWhenPhysicalDeleteFails() {
        ChatAttachmentRepository repository = mock(ChatAttachmentRepository.class);
        FileUploadProperties properties = new FileUploadProperties();
        properties.setChatFileDir(tempDir.toString());
        ChatAttachmentCleanupServiceImpl service = new ChatAttachmentCleanupServiceImpl(repository, properties);
        ChatAttachment invalidPath = attachment(2L, "../outside.txt");
        when(repository.findTop100ByChatMessageIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(invalidPath))
                .thenReturn(List.of());

        service.cleanupOrphanAttachments();

        verify(repository).delete(invalidPath);
    }

    private ChatAttachment attachment(Long id, String fileUrl) {
        ChatAttachment attachment = ChatAttachment.builder()
                .fileUrl(fileUrl)
                .originalFileName("original.txt")
                .storedFileName("stored.txt")
                .contentType("text/plain")
                .fileSize(1L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        ReflectionTestUtils.setField(attachment, "id", id);
        return attachment;
    }
}
