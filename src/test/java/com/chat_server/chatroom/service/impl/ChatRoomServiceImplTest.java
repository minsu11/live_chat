package com.chat_server.chatroom.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomServiceImplTest {
    @Test
    @DisplayName("ChatRoomServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
