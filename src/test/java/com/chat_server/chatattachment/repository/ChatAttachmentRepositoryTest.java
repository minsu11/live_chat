package com.chat_server.chatattachment.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAttachmentRepositoryTest {
    @Test
    @DisplayName("ChatAttachmentRepository는 Repository 계층 인터페이스 계약을 유지한다")
    void repositoryShouldKeepInterfaceContract() {
        assertThat(ChatAttachmentRepository.class.isInterface()).isTrue();
        assertThat(ChatAttachmentRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(ChatAttachmentRepository.class.getPackageName()).contains("repository");
    }
}
