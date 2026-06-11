package com.chat_server.userprofile.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileServiceImplTest {
    @Test
    @DisplayName("UserProfileServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(UserProfileServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(UserProfileServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(UserProfileServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
