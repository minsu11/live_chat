package com.chat_server.chatattachment.controller;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-attachments.prefix}")
public class ChatAttachmentController {
    private final ChatAttachmentService chatAttachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatAttachmentUploadResponse>> upload(
            @RequestParam("roomId") Long roomId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        Long userId = authenticatedUser.userId();
        ChatAttachmentUploadResponse chatAttachmentUploadResponse = chatAttachmentService.upload(roomId,userId,file);
        ApiResponse<ChatAttachmentUploadResponse> response = ApiResponse.success(201,"파일 업로드 완료", chatAttachmentUploadResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        Long userId = authenticatedUser.userId();
        ChatAttachmentDownloadResult result = chatAttachmentService.download(attachmentId,userId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(result.resource());
    }

}
