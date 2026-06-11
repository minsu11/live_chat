package com.chat_server.chatlist.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatListFacadeServiceImplTest {
    @Test
    @DisplayName("ChatListFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatListFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatListFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatListFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
