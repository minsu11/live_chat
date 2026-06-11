package com.chat_server.userblock.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBlockRepositoryCustomImplContractTest {
    @Test
    @DisplayName("UserBlockRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(UserBlockRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(UserBlockRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(UserBlockRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
