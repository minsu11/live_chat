package com.chat_server.logintype.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginTypeRepositoryTest {
    @Test
    @DisplayName("LoginTypeRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(LoginTypeRepository.class.isInterface()).isTrue();
        assertThat(LoginTypeRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(LoginTypeRepository.class.getPackageName()).contains("repository");
    }
}
