package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * chat message facade message service
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
    public class ChatMessageServiceImpl implements ChatMessageFacadeService {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendMessage(ChatSendRequest request, Long userId) {
        // todo 방의 타입 별로 메세지 보내는 기능 따로 놓기

        // room id로 해당 방이 존재하는 지와 개인 채팅인지 그룹 채팅인지 오픈 채팅인지 반환

        // 그 후 조건문을 통해, 채팅 방 타입에 따라서 메서드 실행

        // 메세지 디비 저장한 뒤에는 브로드캐스터 통해서 server에서 클라이언트, 즉 받는 사람에게 알림을 보낼 예정

    }
}
