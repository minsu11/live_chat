package com.chat_server.chatroom.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomQueryServiceImplTest {
    @Test
    @DisplayName("ChatRoomQueryServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomQueryServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomQueryServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomQueryServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
