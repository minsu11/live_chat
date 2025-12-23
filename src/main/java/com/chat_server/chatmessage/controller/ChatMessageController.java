package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping
    public void message(ChatMessage message,
                        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                        ChatSendRequest chatSendRequest
                        ) {
        log.info("chat message controller start");
        log.info("user id: {}", authenticatedUser.userId());
        Long user = authenticatedUser.userId();

    }

}
