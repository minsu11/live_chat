package com.chat_server.websocket.interceptor;

import com.chat_server.security.provider.JwtTokenProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StompAuthChannelInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthorizationService authorizationService;
    private StompAuthChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authorizationService = mock(AuthorizationService.class);
        interceptor = new StompAuthChannelInterceptor(jwtTokenProvider, authorizationService);
        channel = mock(MessageChannel.class);
    }

    /**
     * STOMP 헤더 accessor가 없는 일반 메시지는 인증 처리 대상이 아니므로
     * 원본 메시지를 그대로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("STOMP 인증 경계값 - STOMP accessor가 없는 메시지는 원본을 그대로 반환한다")
    void preSendShouldReturnOriginalMessageWhenAccessorIsMissing() {
        Message<String> message = new GenericMessage<>("payload");

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(jwtTokenProvider, authorizationService);
    }

    /**
     * CONNECT 요청의 Authorization 헤더에서 Bearer 토큰을 추출하고,
     * 검증된 사용자 정보를 Principal로 설정하는 정상 흐름을 검증한다.
     */
    @Test
    @DisplayName("STOMP CONNECT 인증 성공 - 유효한 Bearer 토큰이면 인증 사용자를 Principal로 설정한다")
    void preSendShouldSetAuthenticatedPrincipalWhenConnectTokenIsValid() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer valid-token");
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(10L, "ROLE_USER");

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn("user-uuid");
        when(authorizationService.getAuthorizationUserByUserId("user-uuid"))
                .thenReturn(authenticatedUser);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(Authentication.class);

        Authentication authentication = (Authentication) resultAccessor.getUser();
        assertThat(authentication.getPrincipal()).isEqualTo(authenticatedUser);
        assertThat(authentication.getName()).isEqualTo("10");

        verify(jwtTokenProvider).validateToken("valid-token");
        verify(jwtTokenProvider).getUserId("valid-token");
        verify(authorizationService).getAuthorizationUserByUserId("user-uuid");
    }

    /**
     * CONNECT 요청에 Authorization 헤더가 없는 경우를 검증한다.
     * 토큰 검증 로직을 호출하기 전에 명확한 입력 오류를 반환해야 한다.
     */
    @Test
    @DisplayName("STOMP CONNECT 인증 실패 - Authorization 헤더가 없으면 예외를 발생시킨다")
    void preSendShouldThrowWhenAuthorizationHeaderIsMissing() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization 헤더가 유효하지 않습니다.");

        verifyNoInteractions(jwtTokenProvider, authorizationService);
    }

    /**
     * Authorization 헤더가 존재해도 Bearer 형식이 아닌 경우를 검증한다.
     * 잘못된 인증 스킴으로 전달된 문자열을 JWT로 해석하면 안 된다.
     */
    @Test
    @DisplayName("STOMP CONNECT 인증 실패 - Bearer 형식이 아니면 토큰 검증 전에 예외를 발생시킨다")
    void preSendShouldThrowWhenAuthorizationSchemeIsInvalid() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Basic encoded-value");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization 헤더가 유효하지 않습니다.");

        verifyNoInteractions(jwtTokenProvider, authorizationService);
    }

    /**
     * Bearer 토큰 형식은 맞지만 서명, 만료 등 유효성 검증에 실패한 경우를 검증한다.
     * 실패한 토큰으로 사용자 정보를 조회하거나 Principal을 생성하면 안 된다.
     */
    @Test
    @DisplayName("STOMP CONNECT 인증 실패 - JWT 검증에 실패하면 사용자 조회 없이 예외를 발생시킨다")
    void preSendShouldThrowWhenJwtTokenIsInvalid() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT 토큰이 유효하지 않습니다.");

        verify(jwtTokenProvider, never()).getUserId(anyString());
        verifyNoInteractions(authorizationService);
    }

    /**
     * CONNECT가 아닌 SUBSCRIBE 프레임은 CONNECT 단계에서 설정한 Principal을 사용하므로
     * 추가 JWT 검증 없이 원본 메시지를 통과시키는지 검증한다.
     */
    @Test
    @DisplayName("STOMP SUBSCRIBE 처리 성공 - CONNECT가 아닌 프레임은 추가 인증 없이 통과시킨다")
    void preSendShouldPassSubscribeFrameWithoutAdditionalTokenValidation() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, null);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(jwtTokenProvider, authorizationService);
    }

    private Message<byte[]> stompMessage(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
