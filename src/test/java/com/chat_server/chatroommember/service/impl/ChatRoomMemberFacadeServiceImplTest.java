package com.chat_server.chatroommember.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberFacadeServiceImplTest {
    @Test
    @DisplayName("ChatRoomMemberFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomMemberFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomMemberFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomMemberFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
