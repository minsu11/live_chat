package com.chat_server.chatmessage.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageRepositoryTest {
    @Test
    @DisplayName("ChatMessageRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatMessageRepository.class.isInterface()).isTrue();
        assertThat(ChatMessageRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatMessageRepository.class.getPackageName()).contains("repository");
    }
}
