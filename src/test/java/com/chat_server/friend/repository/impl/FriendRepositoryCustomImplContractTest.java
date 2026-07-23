package com.chat_server.friend.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FriendRepositoryCustomImplContractTest {
    @Test
    @DisplayName("FriendRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(FriendRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(FriendRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(FriendRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
