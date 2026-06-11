package com.chat_server.chatmessage.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageRepositoryCustomTest {
    @Test
    @DisplayName("ChatMessageRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatMessageRepositoryCustom.class.isInterface()).isTrue();
        assertThat(ChatMessageRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatMessageRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
