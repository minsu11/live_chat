package com.chat_server.userprofile.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileFacadeImplTest {
    @Test
    @DisplayName("UserProfileFacadeImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(UserProfileFacadeImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(UserProfileFacadeImpl.class.getInterfaces()).isNotEmpty();
        assertThat(UserProfileFacadeImpl.class.getSimpleName()).endsWith("Impl");
    }
}
