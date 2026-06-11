package com.chat_server.chatlist.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatListRepositoryCustomTest {
    @Test
    @DisplayName("ChatListRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatListRepositoryCustom.class.isInterface()).isTrue();
        assertThat(ChatListRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatListRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
