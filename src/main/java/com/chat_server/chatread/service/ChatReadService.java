package com.chat_server.chatread.service;

import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.request.ChatReadRequest;

public interface ChatReadService {

    /**
     * 실시간 읽음 요청을 처리한다.
     *
     * @param request 읽음 요청 DTO
     * @param userId 요청 사용자 ID
     * @return 읽음 갱신 이벤트 DTO
     */
    ChatReadUpdatedEvent read(ChatReadRequest request, Long userId);
}
