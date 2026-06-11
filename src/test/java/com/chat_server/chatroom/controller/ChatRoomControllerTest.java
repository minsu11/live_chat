package com.chat_server.chatroom.controller;

import com.chat_server.chatmessage.dto.response.ChatMessageContextResponse;
import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;
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

class ChatRoomControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("채팅방 진입 성공 시 200 상태와 진입 정보를 반환한다")
    void enterChatRoomShouldReturnEnterResponse() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        ChatRoomEnterResponse data = new ChatRoomEnterResponse(10L, "GROUP", "room", List.of(), null, false);
        when(service.enterChatRoom(10L, 1L, null, 50)).thenReturn(data);

        ResponseEntity<ApiResponse<ChatRoomEnterResponse>> response = controller.enterChatRoom(user, 10L, null, 50);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("채팅방 진입 정보 조회 성공");
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅방 요약 조회 성공 시 200 상태와 요약 정보를 반환한다")
    void getChatRoomShouldReturnSummary() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        ChatRoomSummaryResponse data = new ChatRoomSummaryResponse(10L, "GROUP", "room", null, 2);
        when(service.getChatRoomSummary(10L, 1L)).thenReturn(data);

        assertThat(controller.getChatRoom(user, 10L).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("1대1 채팅방 생성 성공 시 201 메시지와 생성 결과를 반환한다")
    void createChatRoomShouldReturnChatRoomResult() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        ChatRoomResult data = new ChatRoomResult(11L, true);
        when(service.getOrCreateOneToOneChatRoom(1L, "friend-uuid")).thenReturn(data);

        ResponseEntity<ApiResponse<ChatRoomResult>> response = controller.createChatRoom("friend-uuid", user);

        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅방 메시지 조회 성공 시 200 상태와 메시지 페이지를 반환한다")
    void getChatRoomMessagesShouldReturnMessages() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        ChatRoomEnterResponse data = new ChatRoomEnterResponse(10L, "GROUP", "room", List.of(), null, false);
        when(service.getChatRoomMessages(10L, 1L, "cursor", 30)).thenReturn(data);

        assertThat(controller.getChatRoomMessages(10L, "cursor", 30, user).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("그룹 채팅방 생성 성공 시 201 메시지와 생성 응답을 반환한다")
    void createGroupChatRoomShouldReturnCreateResponse() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest("group", List.of("u2", "u3"));
        CreateChatRoomResponse data = new CreateChatRoomResponse(12L, "GROUP", "group");
        when(service.createGroupChatRoom(1L, request)).thenReturn(data);

        assertThat(controller.createGroupChatRoom(user, request).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("메시지 문맥 조회 성공 시 200 상태와 문맥 메시지를 반환한다")
    void getMessageContextShouldReturnContext() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        ChatMessageContextResponse data = new ChatMessageContextResponse(10L, List.of(), null, null);
        when(service.getMessageContext(10L, 1L, 100L, 50)).thenReturn(data);

        assertThat(controller.getMessageContext(10L, 100L, 50, user).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅방 API 실패 시 서비스 예외를 전파한다")
    void shouldPropagateServiceException() {
        ChatRoomFacadeService service = mock(ChatRoomFacadeService.class);
        ChatRoomController controller = new ChatRoomController(service);
        when(service.enterChatRoom(10L, 1L, null, 50)).thenThrow(new IllegalArgumentException("not a member"));

        assertThatThrownBy(() -> controller.enterChatRoom(user, 10L, null, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not a member");
    }
}
