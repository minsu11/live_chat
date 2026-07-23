package com.chat_server.file.controller;

import com.chat_server.file.dto.response.ChatFileUploadResponse;
import com.chat_server.file.dto.response.ChatImageUploadResponse;
import com.chat_server.file.service.ChatFileFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatFileControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("채팅 이미지 업로드 성공 시 201 상태와 이미지 업로드 응답을 반환한다")
    void uploadChatImageShouldReturnImageResponse() {
        ChatFileFacadeService service = mock(ChatFileFacadeService.class);
        ChatFileController controller = new ChatFileController(service);
        MultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "png".getBytes());
        ChatImageUploadResponse data = new ChatImageUploadResponse("/img", "a.png", "image/png", 3L);
        when(service.uploadChatImage(1L, file)).thenReturn(data);

        var response = controller.uploadChatImage(user, file);

        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅 파일 업로드 성공 시 201 상태와 파일 업로드 응답을 반환한다")
    void uploadChatFileShouldReturnFileResponse() {
        ChatFileFacadeService service = mock(ChatFileFacadeService.class);
        ChatFileController controller = new ChatFileController(service);
        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        ChatFileUploadResponse data = new ChatFileUploadResponse("/file", "a.txt", "text/plain", 4L);
        when(service.uploadChatFile(1L, file)).thenReturn(data);

        var response = controller.uploadChatFile(user, file);

        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅 파일 업로드 실패 시 서비스 예외를 전파한다")
    void uploadChatFileShouldPropagateServiceException() {
        ChatFileFacadeService service = mock(ChatFileFacadeService.class);
        ChatFileController controller = new ChatFileController(service);
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        when(service.uploadChatFile(1L, file)).thenThrow(new IllegalArgumentException("empty"));

        assertThatThrownBy(() -> controller.uploadChatFile(user, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("empty");
    }
}
