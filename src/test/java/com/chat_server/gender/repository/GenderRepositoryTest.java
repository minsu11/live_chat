package com.chat_server.gender.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenderRepositoryTest {
    @Test
    @DisplayName("GenderRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(GenderRepository.class.isInterface()).isTrue();
        assertThat(GenderRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(GenderRepository.class.getPackageName()).contains("repository");
    }
}
