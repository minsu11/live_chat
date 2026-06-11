package com.chat_server.userblock.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBlockRepositoryCustomTest {
    @Test
    @DisplayName("UserBlockRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(UserBlockRepositoryCustom.class.isInterface()).isTrue();
        assertThat(UserBlockRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(UserBlockRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
