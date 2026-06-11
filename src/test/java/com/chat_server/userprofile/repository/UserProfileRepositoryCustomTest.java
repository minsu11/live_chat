package com.chat_server.userprofile.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRepositoryCustomTest {
    @Test
    @DisplayName("UserProfileRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserProfileRepositoryCustom.class.isInterface()).isTrue();
        assertThat(UserProfileRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(UserProfileRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
