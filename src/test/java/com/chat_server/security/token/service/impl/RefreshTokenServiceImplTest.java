package com.chat_server.security.token.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenServiceImplTest {
    @Test
    @DisplayName("RefreshTokenServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(RefreshTokenServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(RefreshTokenServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(RefreshTokenServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
