package com.chat_server.chatread.controller;

import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatReadController {
    private final ChatReadFacadeService chatReadFacadeService;


    @MessageMapping("/chat/read")
    public void read(Authentication authentication,
        @RequestParam  ChatReadRequest request
        ){
        log.info("chat read controller start");
        log.debug("request: {}", request);
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

        Long userId = authenticatedUser.userId();
        chatReadFacadeService.read(request,userId);

    }

}
