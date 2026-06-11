package com.chat_server.chatmessage.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageSearchServiceImplTest {
    @Test
    @DisplayName("ChatMessageSearchServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatMessageSearchServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatMessageSearchServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatMessageSearchServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
