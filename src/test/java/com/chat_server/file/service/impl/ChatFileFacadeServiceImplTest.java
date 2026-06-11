package com.chat_server.file.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatFileFacadeServiceImplTest {
    @Test
    @DisplayName("ChatFileFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatFileFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatFileFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatFileFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
