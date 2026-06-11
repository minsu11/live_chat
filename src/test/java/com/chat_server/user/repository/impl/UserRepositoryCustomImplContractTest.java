package com.chat_server.user.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryCustomImplContractTest {
    @Test
    @DisplayName("UserRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(UserRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(UserRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(UserRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
