package com.chat_server.chatread.service;

import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.request.ChatReadRequest;

public interface ChatReadFacadeService {
    void read(ChatReadRequest request, Long userId);

}
