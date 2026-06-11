package com.chat_server.chatroomsetting.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomSettingFacadeServiceImplTest {
    @Test
    @DisplayName("ChatRoomSettingFacadeServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatRoomSettingFacadeServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatRoomSettingFacadeServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatRoomSettingFacadeServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
