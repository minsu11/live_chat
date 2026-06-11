package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatMessageWsControllerTest {
    @Test
    @DisplayName("웹소켓 메시지 전송 성공 시 인증 사용자 ID로 메시지 서비스를 호출한다")
    void sendMessageShouldDelegateWithAuthenticatedUserId() {
        ChatMessageFacadeService service = mock(ChatMessageFacadeService.class);
        ChatMessageWsController controller = new ChatMessageWsController(service);
        ChatSendRequest request = new ChatSendRequest(10L, MessageType.TEXT, "hello", "client-1");

        controller.sendMessage(request, new TestingAuthenticationToken(new AuthenticatedUser(1L, "USER"), null));

        verify(service).sendMessage(request, 1L);
    }

    @Test
    @DisplayName("웹소켓 메시지 전송 실패 시 principal 타입이 다르면 ClassCastException을 발생시킨다")
    void sendMessageShouldThrowWhenPrincipalTypeIsInvalid() {
        ChatMessageFacadeService service = mock(ChatMessageFacadeService.class);
        ChatMessageWsController controller = new ChatMessageWsController(service);
        ChatSendRequest request = new ChatSendRequest(10L, MessageType.TEXT, "hello", null);

        assertThatThrownBy(() -> controller.sendMessage(request, new TestingAuthenticationToken("invalid", null)))
                .isInstanceOf(ClassCastException.class);

        verify(service, never()).sendMessage(any(), any());
    }
}
