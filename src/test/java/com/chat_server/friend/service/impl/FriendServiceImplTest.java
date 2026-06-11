package com.chat_server.friend.service.impl;

import com.chat_server.common.dto.exception.ConflictException;
import com.chat_server.common.dto.exception.ValidationException;
import com.chat_server.friend.dto.request.UserFriendRegisterRequest;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.friend.entity.Friend;
import com.chat_server.friend.repository.FriendRepository;
import com.chat_server.user.entity.User;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FriendServiceImplTest {
    @Test
    @DisplayName("친구 목록 조회 성공 시 limit를 보정하고 다음 커서를 생성한다")
    void getFriendsByCursorShouldClampLimitAndCreateNextCursor() {
        FriendRepository friendRepository = mock(FriendRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FriendServiceImpl service = new FriendServiceImpl(friendRepository, userRepository);
        UserFriendResponse first = new UserFriendResponse("uuid-a", "Alpha", "profile-a");
        UserFriendResponse last = new UserFriendResponse("uuid-b", "Beta", "profile-b");
        when(friendRepository.getFriendsWithProfileByCursor(eq(1L), eq(50), any()))
                .thenReturn(new SliceImpl<>(List.of(first, last), PageRequest.of(0, 50), true));

        CursorPageResponse<UserFriendResponse> result = service.getFriendsByCursor(1L, 0, null);

        assertThat(result.items()).containsExactly(first, last);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.next()).isNotBlank();
    }

    @Test
    @DisplayName("친구 등록 성공 시 사용자와 친구를 조회하고 중복이 아니면 저장한다")
    void saveFriendShouldSaveWhenUsersAreValidAndNotDuplicated() {
        FriendRepository friendRepository = mock(FriendRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FriendServiceImpl service = new FriendServiceImpl(friendRepository, userRepository);
        User user = user(1L, "user-uuid", "나");
        User friend = user(2L, "friend-uuid", "친구");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUuid("friend-uuid")).thenReturn(Optional.of(friend));
        when(friendRepository.existsByUser_IdAndFriendUser_Id(1L, 2L)).thenReturn(false);

        service.saveFriend(new UserFriendRegisterRequest("friend-uuid"), 1L);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getFriendUser()).isSameAs(friend);
    }

    @Test
    @DisplayName("친구 등록 실패 시 요청 사용자가 없으면 예외를 발생시킨다")
    void saveFriendShouldThrowWhenUserIsMissing() {
        FriendRepository friendRepository = mock(FriendRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FriendServiceImpl service = new FriendServiceImpl(friendRepository, userRepository);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveFriend(new UserFriendRegisterRequest("friend-uuid"), 1L))
                .isInstanceOf(UserNotFoundException.class);
        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("친구 등록 실패 시 자기 자신과 중복 친구를 거부한다")
    void saveFriendShouldRejectSelfAndDuplicatedFriend() {
        FriendRepository friendRepository = mock(FriendRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FriendServiceImpl service = new FriendServiceImpl(friendRepository, userRepository);
        User user = user(1L, "user-uuid", "나");
        User friend = user(2L, "friend-uuid", "친구");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUuid("user-uuid")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.saveFriend(new UserFriendRegisterRequest("user-uuid"), 1L))
                .isInstanceOf(ValidationException.class);

        when(userRepository.findByUuid("friend-uuid")).thenReturn(Optional.of(friend));
        when(friendRepository.existsByUser_IdAndFriendUser_Id(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.saveFriend(new UserFriendRegisterRequest("friend-uuid"), 1L))
                .isInstanceOf(ConflictException.class);

        verify(friendRepository, never()).save(any());
    }

    private User user(Long id, String uuid, String nickname) {
        return User.builder().id(id).uuid(uuid).nickname(nickname).name(nickname)
                .inputId("input-" + id).friendCode("friend-" + id).status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();
    }
}
