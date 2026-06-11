package com.chat_server.chatroom.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomRepositoryCustomTest {
    @Test
    @DisplayName("ChatRoomRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatRoomRepositoryCustom.class.isInterface()).isTrue();
        assertThat(ChatRoomRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatRoomRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
