package com.chat_server.chatread.service.impl;

import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageReadBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatReadFacadeServiceImpl implements ChatReadFacadeService {
    private final ChatReadService chatReadService;
    private final ChatMessageReadBroadcaster chatMessageReadBroadcaster;
    @Override
    public void read(ChatReadRequest request, Long userId) {
        log.info("chat read request");

        ChatReadUpdatedEvent event = chatReadService.read(request,userId);
        chatMessageReadBroadcaster.readRoomBroadcast(request.roomId(), event);
    }
}
