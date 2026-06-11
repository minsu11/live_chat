package com.chat_server.chatread.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatReadServiceImplTest {
    @Test
    @DisplayName("ChatReadServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatReadServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatReadServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatReadServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
