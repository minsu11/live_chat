package com.chat_server.chatlist.controller;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.friend.dto.response.CursorPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatListControllerTest {
    @Test
    @DisplayName("채팅 목록 조회 성공 시 200 상태와 커서 페이지를 반환한다")
    void getChatRoomListShouldReturnCursorPage() {
        ChatListService service = mock(ChatListService.class);
        ChatListController controller = new ChatListController(service);
        CursorPageResponse<ChatRoomListResponse> page = new CursorPageResponse<>(List.of(), null, false);
        when(service.getChatRoomListsByCursor(1L, 50, null)).thenReturn(page);

        ResponseEntity<ApiResponse<CursorPageResponse<ChatRoomListResponse>>> response = controller.getChatRoomList(1L, 50, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(page);
        verify(service).getChatRoomListsByCursor(1L, 50, null);
    }

    @Test
    @DisplayName("채팅 목록 조회 실패 시 서비스 예외를 전파한다")
    void getChatRoomListShouldPropagateServiceException() {
        ChatListService service = mock(ChatListService.class);
        ChatListController controller = new ChatListController(service);
        when(service.getChatRoomListsByCursor(1L, 50, "bad")).thenThrow(new IllegalArgumentException("bad cursor"));

        assertThatThrownBy(() -> controller.getChatRoomList(1L, 50, "bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bad cursor");
    }
}
