package com.chat_server.userprofileImage.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserProfileImageRepositoryImplContractTest {
    @Test
    @DisplayName("CustomUserProfileImageRepositoryImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(CustomUserProfileImageRepositoryImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(CustomUserProfileImageRepositoryImpl.class.getPackageName()).contains("repository.impl");
        assertThat(CustomUserProfileImageRepositoryImpl.class.getInterfaces()).isNotEmpty();
    }
}
