package com.chat_server.chatlist.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatListRepositoryCustomImplContractTest {
    @Test
    @DisplayName("ChatListRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(ChatListRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(ChatListRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(ChatListRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
