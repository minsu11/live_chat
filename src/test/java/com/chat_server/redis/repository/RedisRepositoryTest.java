package com.chat_server.redis.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRepositoryTest {
    @Test
    @DisplayName("RedisRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(RedisRepository.class.isInterface()).isTrue();
        assertThat(RedisRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(RedisRepository.class.getPackageName()).contains("repository");
    }
}
