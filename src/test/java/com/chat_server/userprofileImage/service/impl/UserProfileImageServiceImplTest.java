package com.chat_server.userprofileImage.service.impl;

import com.chat_server.user.entity.User;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.exception.UserProfileNotFoundException;
import com.chat_server.userprofile.repository.UserProfileRepository;
import com.chat_server.userprofileImage.entity.UserProfileImage;
import com.chat_server.userprofileImage.repository.UserProfileImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserProfileImageServiceImplTest {
    @Test
    @DisplayName("프로필 이미지 변경 성공 시 기존 이미지를 비활성화하고 새 이미지를 저장한다")
    void updateUserProfileUrlShouldDeactivateOldImageAndSaveNewImage() {
        UserProfileImageRepository imageRepository = mock(UserProfileImageRepository.class);
        UserProfileRepository profileRepository = mock(UserProfileRepository.class);
        UserProfileImageServiceImpl service = new UserProfileImageServiceImpl(imageRepository, profileRepository, mock(UserRepository.class));
        UserProfile profile = UserProfile.builder().user(user()).stateMessage("state").build();
        UserProfileImage oldImage = UserProfileImage.builder().userProfile(profile).imageUrl("old").current(true).uploadedAt(LocalDateTime.now()).build();
        when(imageRepository.getUserProfileImage(1L)).thenReturn(Optional.of(oldImage));
        when(profileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));

        service.updateUserProfileUrl(1L, "new-url");

        assertThat(oldImage.isCurrent()).isFalse();
        ArgumentCaptor<UserProfileImage> captor = ArgumentCaptor.forClass(UserProfileImage.class);
        verify(imageRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl()).isEqualTo("new-url");
        assertThat(captor.getValue().isCurrent()).isTrue();
        assertThat(captor.getValue().getUserProfile()).isSameAs(profile);
    }

    @Test
    @DisplayName("프로필 이미지 변경 실패 시 사용자 프로필이 없으면 새 이미지를 저장하지 않는다")
    void updateUserProfileUrlShouldThrowWhenProfileIsMissing() {
        UserProfileImageRepository imageRepository = mock(UserProfileImageRepository.class);
        UserProfileRepository profileRepository = mock(UserProfileRepository.class);
        UserProfileImageServiceImpl service = new UserProfileImageServiceImpl(imageRepository, profileRepository, mock(UserRepository.class));
        when(profileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserProfileUrl(1L, "new-url"))
                .isInstanceOf(UserProfileNotFoundException.class);
        verify(imageRepository, never()).save(any());
    }

    @Test
    @DisplayName("프로필 이미지 조회 성공 시 현재 이미지 URL 또는 null을 반환한다")
    void getUserProfileUrlShouldReturnUrlOrNull() {
        UserProfileImageRepository imageRepository = mock(UserProfileImageRepository.class);
        UserProfileImageServiceImpl service = new UserProfileImageServiceImpl(imageRepository, mock(UserProfileRepository.class), mock(UserRepository.class));
        when(imageRepository.getUserProfileImageUrlByUserId(1L)).thenReturn(Optional.of("url"));
        when(imageRepository.getUserProfileImageUrlByUserId(2L)).thenReturn(Optional.empty());

        assertThat(service.getUserProfileUrl(1L)).isEqualTo("url");
        assertThat(service.getUserProfileUrl(2L)).isNull();
    }

    private User user() {
        return User.builder().id(1L).uuid("uuid").nickname("nick").name("name")
                .inputId("input").friendCode("code").status(UserStatus.ACTIVE).build();
    }
}
