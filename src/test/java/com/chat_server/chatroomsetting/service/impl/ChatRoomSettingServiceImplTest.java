package com.chat_server.chatroomsetting.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomSettingServiceImplTest {
    @Test
    @DisplayName("ChatRoomSettingServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomSettingServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomSettingServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomSettingServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
