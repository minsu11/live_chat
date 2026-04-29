package com.chat_server.redis.service;

import com.chat_server.redis.dto.RedisBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    /**
     * 🎯 Redis Topic으로 메시지를 발행(Publish)합니다.
     * @param destination 발행하는 경로
     * @param payload 발행하는 객체 넣을 object
     */
    public void publish(String destination, Object payload ) {
        log.info("public start");
        RedisBroadcastMessage message = new RedisBroadcastMessage(destination, payload);

        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
    }

}
