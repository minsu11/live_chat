package com.chat_server.chatroom.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomRepositoryTest {
    @Test
    @DisplayName("ChatRoomRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatRoomRepository.class.isInterface()).isTrue();
        assertThat(ChatRoomRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatRoomRepository.class.getPackageName()).contains("repository");
    }
}
