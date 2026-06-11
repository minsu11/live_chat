package com.chat_server.userprofile.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRepositoryCustomImplContractTest {
    @Test
    @DisplayName("UserProfileRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(UserProfileRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(UserProfileRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(UserProfileRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
