package com.chat_server.chatattachment.service;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import org.springframework.web.multipart.MultipartFile;

public interface ChatAttachmentService {
    ChatAttachmentUploadResponse upload(Long roomId, Long userId, MultipartFile file);
    void connectMessage(Long attachmentId, Long userId, ChatMessage chatMessage);
    ChatAttachmentDownloadResult download(Long attachmentId, Long userId);
}
