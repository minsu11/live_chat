package com.chat_server.chatattachment.repository;

import com.chat_server.chatattachment.entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    Optional<ChatAttachment> findByIdAndUploader_IdAndChatMessageIsNull(Long id, Long uploaderId);

}
