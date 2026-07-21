package com.chat_server.userprofile.service.impl;

import com.chat_server.file.service.FileService;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserProfileUpdateImageResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateResponse;
import com.chat_server.userprofile.service.UserProfileService;
import com.chat_server.userprofileImage.service.UserProfileImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserProfileFacadeImplTest {

    private FileService fileService;
    private UserService userService;
    private UserProfileService userProfileService;
    private UserProfileImageService userProfileImageService;
    private UserProfileFacadeImpl facade;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        userService = mock(UserService.class);
        userProfileService = mock(UserProfileService.class);
        userProfileImageService = mock(UserProfileImageService.class);
        facade = new UserProfileFacadeImpl(
                fileService,
                userService,
                userProfileService,
                userProfileImageService
        );
    }

    /**
     * 닉네임과 상태 메시지가 모두 전달되면 각각의 도메인 서비스에 값을 전달하고,
     * 변경 요청 값을 응답으로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("프로필 수정 성공 - 닉네임과 상태 메시지가 모두 있으면 두 항목을 각각 수정한다")
    void updateMyProfileShouldUpdateNicknameAndStateMessage() {
        UserProfileUpdateRequest request =
                new UserProfileUpdateRequest("새 닉네임", "새 상태 메시지", null);

        UserProfileUpdateResponse result = facade.updateMyProfile(10L, request);

        verify(userService).updateNickname(10L, "새 닉네임");
        verify(userProfileService).updateStateMessage(10L, "새 상태 메시지");
        assertThat(result.nickName()).isEqualTo("새 닉네임");
        assertThat(result.message()).isEqualTo("새 상태 메시지");
    }

    /**
     * 닉네임만 전달된 부분 수정 요청을 검증한다.
     * 상태 메시지 서비스는 호출하지 않아 기존 값을 유지해야 한다.
     */
    @Test
    @DisplayName("프로필 부분 수정 성공 - 닉네임만 있으면 상태 메시지는 수정하지 않는다")
    void updateMyProfileShouldOnlyUpdateNicknameWhenMessageIsNull() {
        UserProfileUpdateRequest request =
                new UserProfileUpdateRequest("닉네임만 변경", null, null);

        UserProfileUpdateResponse result = facade.updateMyProfile(10L, request);

        verify(userService).updateNickname(10L, "닉네임만 변경");
        verifyNoInteractions(userProfileService);
        assertThat(result.nickName()).isEqualTo("닉네임만 변경");
        assertThat(result.message()).isNull();
    }

    /**
     * 상태 메시지만 전달된 부분 수정 요청을 검증한다.
     * 닉네임 서비스는 호출하지 않아 기존 닉네임을 유지해야 한다.
     */
    @Test
    @DisplayName("프로필 부분 수정 성공 - 상태 메시지만 있으면 닉네임은 수정하지 않는다")
    void updateMyProfileShouldOnlyUpdateStateMessageWhenNameIsNull() {
        UserProfileUpdateRequest request =
                new UserProfileUpdateRequest(null, "메시지만 변경", null);

        UserProfileUpdateResponse result = facade.updateMyProfile(10L, request);

        verifyNoInteractions(userService);
        verify(userProfileService).updateStateMessage(10L, "메시지만 변경");
        assertThat(result.nickName()).isNull();
        assertThat(result.message()).isEqualTo("메시지만 변경");
    }

    /**
     * 수정할 필드가 모두 null인 경계 요청을 검증한다.
     * 불필요한 UPDATE를 만들지 않도록 두 서비스 모두 호출하지 않아야 한다.
     */
    @Test
    @DisplayName("프로필 수정 경계값 - 변경 값이 모두 null이면 저장 서비스 호출을 생략한다")
    void updateMyProfileShouldSkipUpdatesWhenAllValuesAreNull() {
        UserProfileUpdateRequest request =
                new UserProfileUpdateRequest(null, null, null);

        UserProfileUpdateResponse result = facade.updateMyProfile(10L, request);

        verifyNoInteractions(userService, userProfileService);
        assertThat(result.nickName()).isNull();
        assertThat(result.message()).isNull();
    }

    /**
     * 프로필 이미지 저장에 성공하면 반환된 URL을 프로필 이미지 도메인에 반영하고,
     * 동일한 URL을 응답하는지 검증한다.
     */
    @Test
    @DisplayName("프로필 이미지 수정 성공 - 파일 저장 URL을 사용자 프로필 이미지에 반영한다")
    void updateMyProfileImageShouldSaveFileAndUpdateProfileUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(fileService.saveProfileImage(10L, file))
                .thenReturn("/uploads/profile/10/profile.png");

        UserProfileUpdateImageResponse result = facade.updateMyProfileImage(10L, file);

        verify(userProfileImageService)
                .updateUserProfileUrl(10L, "/uploads/profile/10/profile.png");
        assertThat(result.profileUrl()).isEqualTo("/uploads/profile/10/profile.png");
    }

    /**
     * 파일 저장 단계에서 예외가 발생한 실패 흐름을 검증한다.
     * 저장되지 않은 URL로 프로필 데이터를 갱신하면 안 된다.
     */
    @Test
    @DisplayName("프로필 이미지 수정 실패 - 파일 저장에 실패하면 프로필 URL을 갱신하지 않는다")
    void updateMyProfileImageShouldNotUpdateProfileUrlWhenFileSaveFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(fileService.saveProfileImage(10L, file))
                .thenThrow(new IllegalStateException("파일 저장 실패"));

        assertThatThrownBy(() -> facade.updateMyProfileImage(10L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("파일 저장 실패");

        verifyNoInteractions(userProfileImageService);
    }
}
