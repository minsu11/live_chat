package com.chat_server.chatread.controller;

import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatReadControllerTest {
    @Test
    @DisplayName("읽음 웹소켓 요청 성공 시 인증 사용자 ID로 읽음 서비스를 호출한다")
    void readShouldDelegateWithAuthenticatedUserId() {
        ChatReadFacadeService service = mock(ChatReadFacadeService.class);
        ChatReadController controller = new ChatReadController(service);
        ChatReadRequest request = new ChatReadRequest(10L, 100L);

        controller.read(new TestingAuthenticationToken(new AuthenticatedUser(1L, "USER"), null), request);

        verify(service).read(request, 1L);
    }

    @Test
    @DisplayName("읽음 웹소켓 요청 실패 시 principal 타입이 다르면 예외가 발생한다")
    void readShouldThrowWhenPrincipalIsInvalid() {
        ChatReadFacadeService service = mock(ChatReadFacadeService.class);
        ChatReadController controller = new ChatReadController(service);

        assertThatThrownBy(() -> controller.read(new TestingAuthenticationToken("invalid", null), new ChatReadRequest(10L, 100L)))
                .isInstanceOf(ClassCastException.class);

        verify(service, never()).read(any(), any());
    }
}
