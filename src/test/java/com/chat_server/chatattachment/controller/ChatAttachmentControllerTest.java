package com.chat_server.chatattachment.controller;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatattachment.service.ChatAttachmentService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatAttachmentControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("첨부 파일 업로드 성공 시 201 상태와 업로드 응답을 반환한다")
    void uploadShouldReturnCreatedApiResponse() {
        ChatAttachmentService service = mock(ChatAttachmentService.class);
        ChatAttachmentController controller = new ChatAttachmentController(service);
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        ChatAttachmentUploadResponse data = new ChatAttachmentUploadResponse(10L, "a.txt", "text/plain", 5L);
        when(service.upload(100L, 1L, file)).thenReturn(data);

        ResponseEntity<ApiResponse<ChatAttachmentUploadResponse>> response = controller.upload(100L, file, user);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("파일 업로드 완료");
        assertThat(response.getBody().getData()).isEqualTo(data);
        verify(service).upload(100L, 1L, file);
    }

    @Test
    @DisplayName("첨부 파일 다운로드 성공 시 파일 헤더와 리소스를 반환한다")
    void downloadShouldReturnResourceHeaders() {
        ChatAttachmentService service = mock(ChatAttachmentService.class);
        ChatAttachmentController controller = new ChatAttachmentController(service);
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());
        when(service.download(10L, 1L)).thenReturn(new ChatAttachmentDownloadResult(resource, "a.txt", "text/plain", 4L));

        ResponseEntity<?> response = controller.download(10L, user);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4L);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/plain");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).isSameAs(resource);
    }

    @Test
    @DisplayName("첨부 파일 API 실패 시 서비스 예외를 그대로 전파한다")
    void shouldPropagateServiceException() {
        ChatAttachmentService service = mock(ChatAttachmentService.class);
        ChatAttachmentController controller = new ChatAttachmentController(service);
        doThrow(new IllegalStateException("upload failed")).when(service).upload(eq(100L), eq(1L), any());

        assertThatThrownBy(() -> controller.upload(100L, new MockMultipartFile("file", new byte[0]), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("upload failed");
    }
}
