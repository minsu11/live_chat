package com.chat_server.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryCustomTest {
    @Test
    @DisplayName("UserRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserRepositoryCustom.class.isInterface()).isTrue();
        assertThat(UserRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(UserRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
