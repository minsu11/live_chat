package com.chat_server.friend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FriendRepositoryTest {
    @Test
    @DisplayName("FriendRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(FriendRepository.class.isInterface()).isTrue();
        assertThat(FriendRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(FriendRepository.class.getPackageName()).contains("repository");
    }
}
