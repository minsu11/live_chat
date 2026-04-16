package com.chat_server.chatattachment.scheduler;

import com.chat_server.chatattachment.service.ChatAttachmentCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAttachmentCleanupScheduler {

    private final ChatAttachmentCleanupService chatAttachmentCleanupService;

    /**
     * 30분마다 orphan attachment 정리
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupOrphanAttachments() {
        log.info("orphan attachment cleanup start");
        chatAttachmentCleanupService.cleanupOrphanAttachments();
        log.info("orphan attachment cleanup end");
    }
}
