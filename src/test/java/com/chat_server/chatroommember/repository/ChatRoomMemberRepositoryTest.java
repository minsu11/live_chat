package com.chat_server.chatroommember.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberRepositoryTest {
    @Test
    @DisplayName("ChatRoomMemberRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatRoomMemberRepository.class.isInterface()).isTrue();
        assertThat(ChatRoomMemberRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatRoomMemberRepository.class.getPackageName()).contains("repository");
    }
}
