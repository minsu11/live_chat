package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.websocket.broadcaster.ChatMessageBroadCaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageFacadeService messageFacadeService;
    private final ChatMessageBroadCaster chatMessageBroadCaster;

    /**
     * 프론트에서 전송한 메시지를 저장하고 대상자에게 실시간 전파한다.
     *
     * @param chatSendRequest 전송할 메시지 본문(채팅방 ID, 타입, 텍스트 포함)
     * @param authentication 현재 STOMP 세션 인증 정보(내부 principal에 userId 포함)
     * @throws ClassCastException principal 타입이 {@link AuthenticatedUser}가 아닐 경우
     */
    @MessageMapping("chat/message")
    public void sendMessage(
                        ChatSendRequest chatSendRequest,
                        Authentication authentication
                        ) {
        log.info("sendMessage 호출");
        log.debug("sendMessage params - chatSendRequest: {}", chatSendRequest);
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        Long userId= authenticatedUser.userId();
        log.debug("sendMessage auth principal userId: {}", userId);
        messageFacadeService.sendMessage(chatSendRequest,userId);
        log.debug("sendMessage 완료 - roomId: {}, userId: {}", chatSendRequest.roomId(), userId);
    }

    /**
     * 브로드캐스트 요청을 받아 현재 구독 중인 프론트 클라이언트에게 즉시 릴레이한다.
     *
     * @param response 프론트/외부에서 전달한 채팅 메시지 응답 payload
     */
    @MessageMapping("chat/broadcast")
    public void receiveBroadcast(ChatMessageResponse response) {
        log.info("receiveBroadcast 호출");
        log.debug("receiveBroadcast params - response: {}", response);
        chatMessageBroadCaster.relayRoomBroadcast(response);
        log.debug("receiveBroadcast 완료 - roomId: {}", response.roomId());
    }

}
