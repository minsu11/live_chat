package com.chat_server.chatroom.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomRepositoryCustomImplContractTest {
    @Test
    @DisplayName("ChatRoomRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(ChatRoomRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(ChatRoomRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(ChatRoomRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
