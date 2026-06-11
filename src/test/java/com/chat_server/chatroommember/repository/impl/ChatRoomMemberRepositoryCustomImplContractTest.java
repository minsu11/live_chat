package com.chat_server.chatroommember.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberRepositoryCustomImplContractTest {
    @Test
    @DisplayName("ChatRoomMemberRepositoryCustomImpl는 커스텀 Repository 구현체 계약을 유지한다")
    void repositoryImplementationShouldKeepCustomRepositoryContract() {
        assertThat(ChatRoomMemberRepositoryCustomImpl.class.getSimpleName()).endsWith("Impl");
        assertThat(ChatRoomMemberRepositoryCustomImpl.class.getPackageName()).contains("repository.impl");
        assertThat(ChatRoomMemberRepositoryCustomImpl.class.getInterfaces()).isNotEmpty();
    }
}
