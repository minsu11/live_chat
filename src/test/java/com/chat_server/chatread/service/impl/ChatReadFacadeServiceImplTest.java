package com.chat_server.chatread.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatReadFacadeServiceImplTest {
    @Test
    @DisplayName("ChatReadFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatReadFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatReadFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatReadFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
