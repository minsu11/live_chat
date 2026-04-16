package com.chat_server.chatattachment.repository;

import com.chat_server.chatattachment.entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    Optional<ChatAttachment> findByIdAndUploader_IdAndChatMessageIsNull(Long id, Long uploaderId);
    List<ChatAttachment> findTop100ByChatMessageIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(LocalDateTime cutoff);

    boolean existsByChatMessageIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
