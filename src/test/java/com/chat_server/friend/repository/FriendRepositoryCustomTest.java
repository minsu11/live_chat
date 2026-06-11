package com.chat_server.friend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FriendRepositoryCustomTest {
    @Test
    @DisplayName("FriendRepositoryCustom는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(FriendRepositoryCustom.class.isInterface()).isTrue();
        assertThat(FriendRepositoryCustom.class.getSimpleName()).endsWith("Repository");
        assertThat(FriendRepositoryCustom.class.getPackageName()).contains("repository");
    }
}
