package com.chat_server.search.service.impl;

import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {
    @Test
    @DisplayName("사용자 검색 성공 시 친구코드 형태 검색어를 표준 포맷으로 정규화한다")
    void searchUserShouldNormalizeFriendCodeKeyword() {
        UserRepository repository = mock(UserRepository.class);
        SearchServiceImpl service = new SearchServiceImpl(repository);
        SearchUserResponse expected = new SearchUserResponse("uuid-2", "친구", "CTK-ABCDEFGH", null, false, false);
        when(repository.searchUserByFriendCodeOrInputId(1L, "CTK-ABCDEFGH")).thenReturn(expected);

        SearchUserResponse result = service.searchUser(1L, new SearchUserRequest(" ctk abc-def-gh "));

        assertThat(result).isEqualTo(expected);
        verify(repository).searchUserByFriendCodeOrInputId(1L, "CTK-ABCDEFGH");
    }

    @Test
    @DisplayName("사용자 검색 성공 시 일반 키워드는 trim만 적용해서 조회한다")
    void searchUserShouldTrimPlainKeyword() {
        UserRepository repository = mock(UserRepository.class);
        SearchServiceImpl service = new SearchServiceImpl(repository);
        SearchUserResponse expected = new SearchUserResponse("uuid-2", "친구", "CTK-ABCDEFGH", null, false, false);
        when(repository.searchUserByFriendCodeOrInputId(1L, "friend01")).thenReturn(expected);

        assertThat(service.searchUser(1L, new SearchUserRequest(" friend01 "))).isEqualTo(expected);
    }

    @Test
    @DisplayName("사용자 검색 실패 시 결과가 없으면 UserNotFoundException을 발생시킨다")
    void searchUserShouldThrowWhenUserIsNotFound() {
        UserRepository repository = mock(UserRepository.class);
        SearchServiceImpl service = new SearchServiceImpl(repository);
        when(repository.searchUserByFriendCodeOrInputId(1L, "unknown")).thenReturn(null);

        assertThatThrownBy(() -> service.searchUser(1L, new SearchUserRequest("unknown")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");
    }
}
