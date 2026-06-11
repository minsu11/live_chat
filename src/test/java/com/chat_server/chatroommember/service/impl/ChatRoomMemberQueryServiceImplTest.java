package com.chat_server.chatroommember.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberQueryServiceImplTest {
    @Test
    @DisplayName("ChatRoomMemberQueryServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomMemberQueryServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomMemberQueryServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomMemberQueryServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
