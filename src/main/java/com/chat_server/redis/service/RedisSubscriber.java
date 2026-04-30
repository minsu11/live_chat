package com.chat_server.redis.service;

import com.chat_server.redis.dto.RedisBroadcastMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 🎯 Redis에서 메시지가 발행(Publish)되면 이 메서드가 자동으로 실행됩니다.
     * @param publishMessage Redis로부터 받은 JSON 문자열
     */
    public void sendMessage(String publishMessage) {
        try {
            log.info("redis subscriber 호출");
            // 1. 넘어온 JSON 문자열을 우리가 만든 DTO로 변환(역직렬화)
            RedisBroadcastMessage message = objectMapper.readValue(publishMessage, RedisBroadcastMessage.class);

            // 2. 목적지(destination)를 확인하고, 웹소켓(STOMP)으로 연결된 유저들에게 쏴줌!
            messagingTemplate.convertAndSend(message.destination(), message.payload());

        } catch (Exception e) {
            log.error("Redis Subscriber 역직렬화 또는 웹소켓 전송 실패: {}", e.getMessage(), e);
        }
    }}
