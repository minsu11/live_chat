package com.chat_server.search.controller;

import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.search.service.SearchService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SearchControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("사용자 검색 성공 시 200 상태와 검색 결과를 반환한다")
    void searchUserShouldReturnSearchResult() {
        SearchService service = mock(SearchService.class);
        SearchController controller = new SearchController(service);
        SearchUserRequest request = new SearchUserRequest("CTK-ABCDEFGH");
        SearchUserResponse data = new SearchUserResponse("u2", "friend", "CTK-ABCDEFGH", null, false, false);
        when(service.searchUser(1L, request)).thenReturn(data);

        var response = controller.searchUser(user, request);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("검색에 성공했습니다.");
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("사용자 검색 실패 시 서비스 예외를 전파한다")
    void searchUserShouldPropagateServiceException() {
        SearchService service = mock(SearchService.class);
        SearchController controller = new SearchController(service);
        SearchUserRequest request = new SearchUserRequest("unknown");
        when(service.searchUser(1L, request)).thenThrow(new IllegalArgumentException("not found"));

        assertThatThrownBy(() -> controller.searchUser(user, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not found");
    }
}
