package com.chat_server.user.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class UserDisplayNameServiceImplTest {
    @Test
    @DisplayName("UserDisplayNameServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(UserDisplayNameServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(UserDisplayNameServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(UserDisplayNameServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
