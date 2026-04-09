package com.chat_server.file.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.file.dto.response.ChatImageUploadResponse;
import com.chat_server.file.service.ChatFileFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-file.prefix}")
public class ChatFileController {
    private final ChatFileFacadeService fileFacadeService;

    @PostMapping(value ="/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatImageUploadResponse>> uploadChatImage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestPart("file") @NotNull MultipartFile file
    ){
        log.info("chat image upload start");
        Long userId = authenticatedUser.userId();
        ChatImageUploadResponse chatImageUploadResponse = fileFacadeService.uploadChatImage(userId, file);
        ApiResponse<ChatImageUploadResponse> response = ApiResponse.success(201,"파일 저장 완료", chatImageUploadResponse);
        return ResponseEntity.ok(response);
    }
}
