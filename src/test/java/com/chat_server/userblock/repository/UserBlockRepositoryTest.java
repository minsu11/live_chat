package com.chat_server.userblock.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBlockRepositoryTest {
    @Test
    @DisplayName("UserBlockRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserBlockRepository.class.isInterface()).isTrue();
        assertThat(UserBlockRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(UserBlockRepository.class.getPackageName()).contains("repository");
    }
}
