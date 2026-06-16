package com.chat_server.chatattachment.scheduler;

import com.chat_server.chatattachment.service.ChatAttachmentCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatAttachmentCleanupSchedulerTest {
    @Test
    @DisplayName("스케줄러 실행 성공 시 orphan cleanup 서비스를 호출한다")
    void cleanupOrphanAttachmentsShouldDelegateToService() {
        ChatAttachmentCleanupService service = mock(ChatAttachmentCleanupService.class);
        ChatAttachmentCleanupScheduler scheduler = new ChatAttachmentCleanupScheduler(service);

        scheduler.cleanupOrphanAttachments();

        verify(service).cleanupOrphanAttachments();
    }

    @Test
    @DisplayName("스케줄러 실행 실패 시 cleanup 서비스 예외를 전파한다")
    void cleanupOrphanAttachmentsShouldPropagateServiceException() {
        ChatAttachmentCleanupService service = mock(ChatAttachmentCleanupService.class);
        ChatAttachmentCleanupScheduler scheduler = new ChatAttachmentCleanupScheduler(service);
        doThrow(new IllegalStateException("cleanup failed")).when(service).cleanupOrphanAttachments();

        assertThatThrownBy(scheduler::cleanupOrphanAttachments)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cleanup failed");
    }
}
