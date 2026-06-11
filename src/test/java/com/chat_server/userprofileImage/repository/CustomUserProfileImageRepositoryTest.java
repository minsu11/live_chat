package com.chat_server.userprofileImage.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserProfileImageRepositoryTest {
    @Test
    @DisplayName("CustomUserProfileImageRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(CustomUserProfileImageRepository.class.isInterface()).isTrue();
        assertThat(CustomUserProfileImageRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(CustomUserProfileImageRepository.class.getPackageName()).contains("repository");
    }
}
