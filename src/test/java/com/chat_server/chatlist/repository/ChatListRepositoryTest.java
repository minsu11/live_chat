package com.chat_server.chatlist.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatListRepositoryTest {
    @Test
    @DisplayName("ChatListRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatListRepository.class.isInterface()).isTrue();
        assertThat(ChatListRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatListRepository.class.getPackageName()).contains("repository");
    }
}
