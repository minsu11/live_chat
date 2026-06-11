package com.chat_server.chatmessage.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageSearchFacadeServiceImplTest {
    @Test
    @DisplayName("ChatMessageSearchFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatMessageSearchFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatMessageSearchFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatMessageSearchFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
