package com.chat_server.redis.service;

import com.chat_server.redis.dto.RedisBroadcastMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;
    private final SimpMessagingTemplate messagingTemplate;


    /**
     * 🎯 Redis Topic으로 메시지를 발행(Publish)합니다.
     * @param destination 발행하는 경로
     * @param payload 발행하는 객체 넣을 object
     */

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "publishFallback")
    public void publish(String destination, Object payload) {
        // 🚀 정상 흐름: 에러가 나면 서킷 브레이커가 낚아채서 즉시 아래의 publishFallback으로 보냅니다.
        RedisBroadcastMessage message = new RedisBroadcastMessage(destination, payload);
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);

        log.debug("✅ Redis Pub/Sub 브로드캐스트 성공: {}", channelTopic.getTopic());
    }

    /**
     * 🚨 장애 발생 시 실행되는 Fallback (우회) 메서드
     * 서킷 브레이커가 이 메서드를 대신 실행해주므로, 메인 트랜잭션은 롤백되지 않고 DB에 정상 커밋됩니다!
     */
    private void publishFallback(String destination, Object payload, Throwable t) {
        log.warn("🚨 [CircuitBreaker] Redis Pub/Sub 장애! 로컬 STOMP로 고속 우회합니다. 사유: {}", t.getMessage());

        // 🔥 우아한 성능 저하 (Graceful Degradation):
        // 기존의 catch 블록에 있던 로컬 웹소켓 브로드캐스트 로직을 이쪽으로 옮깁니다.
        if (destination != null) {
            try {
                messagingTemplate.convertAndSend(destination, payload);
            } catch (Exception innerEx) {
                log.error("❌ 로컬 브로드캐스트마저 실패했습니다.", innerEx);
            }
        }
    }

}
