package com.chat_server.websocket.broadcaster.chatmessage.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListEventBroadcasterImpl implements ChatListEventBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void broadcastUpsertToUser(Long userId, ChatListUpsertEvent event) {
        if (userId == null) {
            log.warn("chat list broadcast skipped. userId is null. event={}", event);
            return;
        }

        if (event == null) {
            log.warn("chat list broadcast skipped. event is null. userId={}", userId);
            return;
        }

        log.info("broadcast chat list event. userId={}, roomId={}", userId, event.roomId());
        String destination = webSocketProperties.getSubPrefix()
            + webSocketProperties.getChat().getChatList();
        messagingTemplate.convertAndSendToUser(
            String.valueOf(userId),
            destination,
            event
        );
    }
}
