package com.chat_server.userprofile.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRepositoryTest {
    @Test
    @DisplayName("UserProfileRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserProfileRepository.class.isInterface()).isTrue();
        assertThat(UserProfileRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(UserProfileRepository.class.getPackageName()).contains("repository");
    }
}
