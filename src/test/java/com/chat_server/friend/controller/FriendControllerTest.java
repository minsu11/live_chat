package com.chat_server.friend.controller;

import com.chat_server.friend.dto.request.UserFriendRegisterRequest;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.friend.service.FriendService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FriendControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("친구 목록 조회 성공 시 200 상태와 친구 페이지를 반환한다")
    void getFriendsShouldReturnFriendPage() {
        FriendService service = mock(FriendService.class);
        FriendController controller = new FriendController(service);
        CursorPageResponse<UserFriendResponse> data = new CursorPageResponse<>(List.of(new UserFriendResponse("u2", "friend", null)), null, false);
        when(service.getFriendsByCursor(1L, 50, null)).thenReturn(data);

        var response = controller.getFriends(user, 50, null);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("친구 목록을 반환합니다.");
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("친구 등록 성공 시 201 상태를 반환하고 서비스를 호출한다")
    void registerShouldReturnCreated() {
        FriendService service = mock(FriendService.class);
        FriendController controller = new FriendController(service);
        UserFriendRegisterRequest request = new UserFriendRegisterRequest("u2");

        var response = controller.register(user, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        verify(service).saveFriend(request, 1L);
    }

    @Test
    @DisplayName("친구 등록 실패 시 서비스 예외를 전파한다")
    void registerShouldPropagateServiceException() {
        FriendService service = mock(FriendService.class);
        FriendController controller = new FriendController(service);
        UserFriendRegisterRequest request = new UserFriendRegisterRequest("u2");
        doThrow(new IllegalStateException("duplicated")).when(service).saveFriend(request, 1L);

        assertThatThrownBy(() -> controller.register(user, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("duplicated");
    }
}
