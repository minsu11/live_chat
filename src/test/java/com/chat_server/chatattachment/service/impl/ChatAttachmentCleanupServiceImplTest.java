package com.chat_server.chatattachment.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAttachmentCleanupServiceImplTest {
    @Test
    @DisplayName("ChatAttachmentCleanupServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(ChatAttachmentCleanupServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ChatAttachmentCleanupServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(ChatAttachmentCleanupServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
