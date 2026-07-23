package com.chat_server.userprofileImage.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileImageRepositoryTest {
    @Test
    @DisplayName("UserProfileImageRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserProfileImageRepository.class.isInterface()).isTrue();
        assertThat(UserProfileImageRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(UserProfileImageRepository.class.getPackageName()).contains("repository");
    }
}
