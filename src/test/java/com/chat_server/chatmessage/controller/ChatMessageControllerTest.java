package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.response.ChatMessageCatchUpResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatMessageControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("누락 메시지 조회 성공 시 200 상태와 catch-up 응답을 반환한다")
    void getMessagesAfterShouldReturnCatchUpResponse() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatMessageController controller = new ChatMessageController(service);
        ChatMessageCatchUpResponse data = new ChatMessageCatchUpResponse(10L, List.of(), false, 100L);
        when(service.getMessagesAfter(10L, 1L, 90L, 50)).thenReturn(data);

        ResponseEntity<ApiResponse<ChatMessageCatchUpResponse>> response = controller.getMessagesAfter(10L, 90L, 50, user);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("누락 메시지 복구 조회 성공");
        assertThat(response.getBody().getData()).isEqualTo(data);
        verify(service).getMessagesAfter(10L, 1L, 90L, 50);
    }

    @Test
    @DisplayName("누락 메시지 조회 실패 시 서비스 예외를 전파한다")
    void getMessagesAfterShouldPropagateException() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatMessageController controller = new ChatMessageController(service);
        when(service.getMessagesAfter(10L, 1L, 90L, 50)).thenThrow(new IllegalStateException("room error"));

        assertThatThrownBy(() -> controller.getMessagesAfter(10L, 90L, 50, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("room error");
    }
}
