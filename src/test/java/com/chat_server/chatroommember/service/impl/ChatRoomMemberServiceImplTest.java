package com.chat_server.chatroommember.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberServiceImplTest {
    @Test
    @DisplayName("ChatRoomMemberServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomMemberServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomMemberServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomMemberServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
