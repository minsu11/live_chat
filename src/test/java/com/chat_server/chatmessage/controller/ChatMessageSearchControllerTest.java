package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchPageResponse;
import com.chat_server.chatmessage.service.ChatMessageSearchFacadeService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatMessageSearchControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("메시지 검색 성공 시 200 상태와 검색 페이지를 반환한다")
    void searchMessagesShouldReturnSearchPage() {
        ChatMessageSearchFacadeService service = mock(ChatMessageSearchFacadeService.class);
        ChatMessageSearchController controller = new ChatMessageSearchController(service);
        ChatMessageSearchPageResponse data = new ChatMessageSearchPageResponse(List.of(), null, false);
        when(service.searchChatMessages(10L, 1L, "hello", null, 50)).thenReturn(data);

        ResponseEntity<ApiResponse<ChatMessageSearchPageResponse>> response = controller.searchMessages(10L, "hello", null, 50, user);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("검색 결과 조회 성공");
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("메시지 검색 실패 시 서비스 예외를 전파한다")
    void searchMessagesShouldPropagateException() {
        ChatMessageSearchFacadeService service = mock(ChatMessageSearchFacadeService.class);
        ChatMessageSearchController controller = new ChatMessageSearchController(service);
        when(service.searchChatMessages(10L, 1L, "", null, 50)).thenThrow(new IllegalArgumentException("keyword required"));

        assertThatThrownBy(() -> controller.searchMessages(10L, "", null, 50, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("keyword required");
    }
}
