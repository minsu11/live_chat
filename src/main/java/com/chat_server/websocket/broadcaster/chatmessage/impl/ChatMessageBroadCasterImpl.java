package com.chat_server.websocket.broadcaster.chatmessage.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.redis.service.RedisPublisher;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageBroadCaster;
import com.chat_server.websocket.properties.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadCasterImpl implements ChatMessageBroadCaster {
    private final RedisPublisher redisPublisher;
    private final WebSocketProperties webSocketProperties;


    /**
     * 수신자 사용자 전용 큐로 채팅 메시지를 전송한다.
     *
     * <p>destination 정책:
     * {@code /user + subPrefix + roomPath + /{roomId}}
     *
     * @param receiverUserId 수신자 사용자 ID
     * @param response 전송할 메시지 응답 DTO
     */
    @Override
    public void broadcastMessage(Long receiverUserId, ChatMessageResponse response) {
        // 사용자별 display nickname/mine 값을 반영하기 위해 사용자 전용 큐로 전송한다.
        log.info("broadcastMessage 호출");
        log.debug("broadcastMessage params - receiverUserId: {}, response: {}", receiverUserId, response);
        String userDestination = webSocketProperties.getSubPrefix()
                + webSocketProperties.getChat().getRoomPath()
                + "/" + response.roomId();
        log.debug("userDestination: {}", userDestination);
        String destination = "/user/"+receiverUserId+ userDestination;
        redisPublisher.publish(destination,response);
        log.debug("broadcastMessage 완료 - userDestination: {}", destination);
    }

    /**
     * 룸 구독 경로로 브로드캐스트를 릴레이한다.
     *
     * @param response 릴레이할 메시지 응답 DTO
     */
    @Override
    public void relayRoomBroadcast(ChatMessageResponse response) {
        // 프론트/외부에서 전달된 브로드캐스트 요청을 채팅방 구독 경로로 릴레이한다.
        log.info("relayRoomBroadcast 호출");
        log.debug("relayRoomBroadcast params - response: {}", response);
        String roomDestination = webSocketProperties.getSubPrefix()
                + webSocketProperties.getChat().getRoomPath()
                + "/" + response.roomId();

        redisPublisher.publish(roomDestination, response);
        log.debug("relayRoomBroadcast 완료 - roomDestination: {}", roomDestination);
    }


}
