package com.chat_server.chatattachment.service.impl;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.chatmessage.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatAttachmentServiceImpl implements ChatAttachmentService {


    @Override
    public ChatAttachmentUploadResponse upload(Long roomId, Long userId, MultipartFile file) {
        return null;
    }

    @Override
    public void connectMessage(Long attachmentId, Long userId, ChatMessage chatMessage) {

    }

    @Override
    public ChatAttachmentDownloadResult download(Long attachmentId, Long userId) {
        return null;
    }
}
