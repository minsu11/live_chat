package com.chat_server.userprofile.service.impl;

import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceImplBehaviorTest {

    private UserRepository userRepository;
    private UserProfileRepository userProfileRepository;
    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        service = new UserProfileServiceImpl(userRepository, userProfileRepository);
    }

    /** 본인 프로필 요약 조회 성공 시 Repository 결과를 그대로 반환하는지 검증한다. */
    @Test
    @DisplayName("본인 프로필 요약 조회 성공 - Repository 결과를 그대로 반환한다")
    void getMyProfileSummaryShouldReturnRepositoryResult() {
        UserMyProfileSummaryResponse expected =
                new UserMyProfileSummaryResponse("uuid-1", "민수", "F001", "상태", "/profile.png");
        when(userProfileRepository.findMyProfile(1L)).thenReturn(Optional.of(expected));

        UserMyProfileSummaryResponse result = service.getMyProfileSummary(1L);

        assertThat(result).isSameAs(expected);
    }

    /** 프로필 요약이 없으면 명확한 도메인 예외가 발생하는지 검증한다. */
    @Test
    @DisplayName("본인 프로필 요약 조회 실패 - 프로필이 없으면 UserNotFoundException을 발생시킨다")
    void getMyProfileSummaryShouldThrowWhenProfileMissing() {
        when(userProfileRepository.findMyProfile(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfileSummary(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("유저 프로필");
    }

    /** 본인 상세 프로필 정상 조회를 검증한다. */
    @Test
    @DisplayName("본인 상세 프로필 조회 성공 - 상세 응답을 반환한다")
    void getMyProfileDetailShouldReturnRepositoryResult() {
        UserProfileDetailResponse expected =
                new UserProfileDetailResponse("uuid-1", "민수", "F001", "상태", "/p.png", false, true);
        when(userProfileRepository.findProfileDetail(1L)).thenReturn(Optional.of(expected));

        assertThat(service.getMyProfileDetail(1L)).isSameAs(expected);
    }

    /** 본인 상세 프로필이 없을 때 실패하는지 검증한다. */
    @Test
    @DisplayName("본인 상세 프로필 조회 실패 - 상세 프로필이 없으면 예외를 발생시킨다")
    void getMyProfileDetailShouldThrowWhenMissing() {
        when(userProfileRepository.findProfileDetail(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfileDetail(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    /** 다른 사용자의 UUID 기반 상세 프로필 정상 조회를 검증한다. */
    @Test
    @DisplayName("사용자 상세 프로필 조회 성공 - viewerId와 대상 UUID로 조회한다")
    void getProfileDetailByUuidShouldReturnRepositoryResult() {
        UserProfileDetailResponse expected =
                new UserProfileDetailResponse("target-uuid", "친구", "F002", "메시지", null, true, false);
        when(userProfileRepository.findProfileDetailByUuid(1L, "target-uuid"))
                .thenReturn(Optional.of(expected));

        assertThat(service.getProfileDetailByUuid(1L, "target-uuid")).isSameAs(expected);
    }

    /** 대상 UUID가 존재하지 않는 경우의 실패 경로를 검증한다. */
    @Test
    @DisplayName("사용자 상세 프로필 조회 실패 - 대상 UUID 프로필이 없으면 예외를 발생시킨다")
    void getProfileDetailByUuidShouldThrowWhenMissing() {
        when(userProfileRepository.findProfileDetailByUuid(1L, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileDetailByUuid(1L, "missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    /** 상태 메시지가 빈 문자열이어도 그대로 반영되는 경계값을 검증한다. */
    @Test
    @DisplayName("상태 메시지 수정 경계값 - 빈 문자열도 프로필 엔티티에 반영한다")
    void updateStateMessageShouldAllowEmptyString() {
        UserProfile profile = UserProfile.builder().stateMessage("기존").user(user(1L)).build();
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));

        service.updateStateMessage(1L, "");

        assertThat(profile.getStateMessage()).isEmpty();
    }

    /** 상태 메시지 수정 대상 프로필이 없는 실패 경로를 검증한다. */
    @Test
    @DisplayName("상태 메시지 수정 실패 - 사용자 프로필이 없으면 예외를 발생시킨다")
    void updateStateMessageShouldThrowWhenProfileMissing() {
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStateMessage(1L, "새 메시지"))
                .isInstanceOf(UserNotFoundException.class);
    }

    /** 사용자 생성 후 빈 상태 메시지의 프로필을 저장하는지 검증한다. */
    @Test
    @DisplayName("사용자 프로필 생성 성공 - 사용자와 빈 상태 메시지를 가진 프로필을 저장한다")
    void createUserProfileShouldSaveInitialProfile() {
        User user = user(1L);
        when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(user));

        service.createUserProfile("uuid-1");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getStateMessage()).isEmpty();
    }

    /** 사용자 UUID가 존재하지 않으면 프로필 저장을 중단하는지 검증한다. */
    @Test
    @DisplayName("사용자 프로필 생성 실패 - 사용자 UUID가 없으면 저장하지 않는다")
    void createUserProfileShouldThrowWhenUserMissing() {
        when(userRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUserProfile("missing"))
                .isInstanceOf(UserNotFoundException.class);

        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .uuid("uuid-" + id)
                .nickname("user-" + id)
                .name("user-" + id)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }
}
