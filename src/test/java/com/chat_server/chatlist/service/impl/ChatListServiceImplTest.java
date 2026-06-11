package com.chat_server.chatlist.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatListServiceImplTest {
    @Test
    @DisplayName("ChatListServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatListServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatListServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatListServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
