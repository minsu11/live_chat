package com.chat_server.websocket.interceptor;

import com.chat_server.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) return message;

        // CONNECT: 최초 WebSocket 연결 시 Authorization 헤더 검사
        if (StompCommand.CONNECT.equals(command)) {
            String rawToken = accessor.getFirstNativeHeader("Authorization");

            if (rawToken == null || !rawToken.startsWith("Bearer ")) {
                log.warn("❌ WebSocket 인증 실패: Authorization 헤더 없음 또는 Bearer 형식 아님");
                throw new IllegalArgumentException("Authorization 헤더가 유효하지 않습니다.");
            }

            String token = rawToken.substring(7); // "Bearer " 제거

            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("❌ WebSocket 인증 실패: 토큰 유효성 검사 실패");
                throw new IllegalArgumentException("JWT 토큰이 유효하지 않습니다.");
            }

            // 사용자 식별 정보 저장 (옵션: 세션에 사용자 ID 저장)
            String userId = jwtTokenProvider.getUserId(token);
            accessor.setUser(() -> userId);
            log.info("✅ WebSocket CONNECT 인증 성공 - userId: {}", userId);
        }

        return message;
    }
}
