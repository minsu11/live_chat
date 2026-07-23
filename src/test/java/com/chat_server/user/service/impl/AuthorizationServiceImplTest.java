package com.chat_server.user.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceImplTest {
    @Test
    @DisplayName("AuthorizationServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(AuthorizationServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(AuthorizationServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(AuthorizationServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
