package com.chat_server.chatmessage.controller;

import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageWsController {

    private final ChatMessageFacadeService messageFacadeService;
    /**
     * 프론트에서 전송한 메시지를 저장하고 대상자에게 실시간 전파한다.
     *
     * @param chatSendRequest 전송할 메시지 본문(채팅방 ID, 타입, 텍스트 포함)
     * @param authentication 현재 STOMP 세션 인증 정보(내부 principal에 userId 포함)
     * @throws ClassCastException principal 타입이 {@link AuthenticatedUser}가 아닐 경우
     */
    @MessageMapping("/chat/message")
    public void sendMessage(
                        ChatSendRequest chatSendRequest,
                        Authentication authentication
                        ) {
        log.info("sendMessage 호출");
        log.debug("sendMessage params - chatSendRequest: {}", chatSendRequest);
        log.debug("authentication: {}", authentication);
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
     * @param authentication 현재 STOMP 세션 인증 정보(내부 principal에 userId 포함)
     * @throws IllegalArgumentException payload senderId와 인증 사용자 ID가 일치하지 않을 경우
     */
    /**
     * 현재 구조에서는 사용하지 않는다.
     *
     * 이유:
     * - 클라이언트는 /chat/message 로만 SEND 한다.
     * - 서버가 메시지 저장 후 broadcaster를 통해 구독자에게 직접 전파한다.
     * - 따라서 응답 DTO(ChatMessageResponse)를 입력 DTO로 재사용할 필요가 없다.
     *
     * 추후
     * - 클라이언트가 "브로드캐스트 요청"을 별도로 보내는 구조가 필요할 때
     * - 별도 요청 DTO(ChatBroadcastRequest)를 정의해서 다시 활성화한다.
     */
    /*
    @MessageMapping("/chat/broadcast")
    public void receiveBroadcast(ChatMessageResponse response, Authentication authentication) {
        log.info("receiveBroadcast 호출");
        log.debug("receiveBroadcast params - response: {}, authentication: {}", response, authentication);

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        Long userId = authenticatedUser.userId();
        log.debug("receiveBroadcast authenticated userId: {}", userId);

        if (!userId.equals(response.sender().senderId())) {
            log.info("receiveBroadcast senderId 불일치");
            log.debug("receiveBroadcast invalid sender - tokenUserId: {}, payloadSenderId: {}",
                    userId, response.sender().senderId());
            throw new IllegalArgumentException("senderId does not match authenticated user");
        }

        chatRoomQueryService.validateMemberOrThrow(response.roomId(), userId);

        chatMessageBroadCaster.relayRoomBroadcast(response);
        log.debug("receiveBroadcast 완료 - roomId: {}, userId: {}", response.roomId(), userId);
    }
    */

}
