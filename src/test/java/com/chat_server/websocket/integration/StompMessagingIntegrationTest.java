package com.chat_server.websocket.integration;

import com.chat_server.chatmessage.controller.ChatMessageWsController;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.security.provider.JwtTokenProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import com.chat_server.websocket.config.WebSocketConfig;
import com.chat_server.websocket.interceptor.StompAuthChannelInterceptor;
import com.chat_server.websocket.properties.WebSocketProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = StompMessagingIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "custom.web-socket.sub-prefix=/api/sub",
                "custom.web-socket.pub-prefix=/api/pub",
                "custom.web-socket.end-point=/api/ws-chat"
        }
)
class StompMessagingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ChatMessageFacadeService chatMessageFacadeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {

        reset(
                jwtTokenProvider,
                authorizationService,
                chatMessageFacadeService
        );

        SockJsClient sockJsClient = new SockJsClient(
                List.of(
                        new WebSocketTransport(
                                new StandardWebSocketClient()
                        )
                )
        );

        stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter =
                new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);

        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {

        for (StompSession session : sessions) {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        sessions.clear();

        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName(
            "STOMP 통합 - 인증 후 구독한 사용자가 전송한 메시지를 실제 MESSAGE frame으로 수신한다"
    )
    void sendAndReceiveMessageThroughStomp() throws Exception {

        // given
        Long userId = 1L;
        Long roomId = 10L;

        String token = "integration-token";
        String userUuid = "integration-user-uuid";

        when(jwtTokenProvider.validateToken(token))
                .thenReturn(true);

        when(jwtTokenProvider.getUserId(token))
                .thenReturn(userUuid);

        when(
                authorizationService
                        .getAuthorizationUserByUserId(userUuid)
        ).thenReturn(
                new AuthenticatedUser(
                        userId,
                        "ROLE_USER"
                )
        );

        /*
         * 이번 테스트에서는 DB/Redis 계층까지 검증하지 않는다.
         *
         * Controller가 Facade를 호출하면
         * 실제 Spring STOMP broker를 통해 응답을 전달하도록 한다.
         */
        doAnswer(invocation -> {

            ChatSendRequest request =
                    invocation.getArgument(0);

            Long authenticatedUserId =
                    invocation.getArgument(1);

            ChatMessageResponse response =
                    new ChatMessageResponse(
                            999L,
                            request.roomId(),
                            request.clientMessageId(),
                            request.messageType().name(),
                            new ChatMessageSenderResponse(
                                    userUuid,
                                    "integration-user",
                                    null
                            ),
                            request.messageContent(),
                            LocalDateTime.now(),
                            true,
                            0
                    );

            String destination =
                    "/user/"
                            + authenticatedUserId
                            + "/api/sub/chat/rooms/"
                            + request.roomId();

            messagingTemplate.convertAndSend(
                    destination,
                    response
            );

            return null;

        }).when(chatMessageFacadeService)
                .sendMessage(
                        any(ChatSendRequest.class),
                        eq(userId)
                );

        BlockingQueue<ChatMessageResponse> receivedMessages =
                new LinkedBlockingQueue<>();


        // when - CONNECT
        StompHeaders connectHeaders =
                new StompHeaders();

        connectHeaders.add(
                "Authorization",
                "Bearer " + token
        );

        String url =
                "http://localhost:"
                        + port
                        + "/api/ws-chat";

        stompSession = stompClient
                .connectAsync(
                        url,
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        }
                )
                .get(
                        5,
                        TimeUnit.SECONDS
                );


        // SUBSCRIBE
        stompSession.subscribe(
                "/user/api/sub/chat/rooms/" + roomId,
                new StompFrameHandler() {

                    @Override
                    public Type getPayloadType(
                            StompHeaders headers
                    ) {
                        return ChatMessageResponse.class;
                    }

                    @Override
                    public void handleFrame(
                            StompHeaders headers,
                            Object payload
                    ) {

                        receivedMessages.offer(
                                (ChatMessageResponse) payload
                        );
                    }
                }
        );


        /*
         * subscribe frame이 broker에 등록될 시간을 아주 조금 확보한다.
         * 첫 성공 후 receipt 기반 대기로 개선한다.
         */
        Thread.sleep(200);


        // SEND
        ChatSendRequest request =
                new ChatSendRequest(
                        roomId,
                        MessageType.TEXT,
                        "STOMP_INTEGRATION_TEST",
                        "client-stomp-it-001"
                );

        stompSession.send(
                "/api/pub/chat/message",
                request
        );


        // then
        ChatMessageResponse received =
                receivedMessages.poll(
                        3,
                        TimeUnit.SECONDS
                );

        assertThat(received)
                .isNotNull();

        assertThat(received.roomId())
                .isEqualTo(roomId);

        assertThat(received.clientMessageId())
                .isEqualTo("client-stomp-it-001");

        assertThat(received.content())
                .isEqualTo("STOMP_INTEGRATION_TEST");

        assertThat(received.messageType())
                .isEqualTo("TEXT");


        verify(
                chatMessageFacadeService,
                timeout(1000)
        ).sendMessage(
                argThat(message ->
                        message.roomId().equals(roomId)
                                && message.messageContent()
                                .equals("STOMP_INTEGRATION_TEST")
                ),
                eq(userId)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(
            exclude = SecurityAutoConfiguration.class
    )
    @Import({
            WebSocketConfig.class,
            WebSocketProperties.class,
            StompAuthChannelInterceptor.class,
            ChatMessageWsController.class,
            TestBeans.class
    })
    static class TestApplication {
    }


    @TestConfiguration
    static class TestBeans {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return mock(AuthorizationService.class);
        }

        @Bean
        ChatMessageFacadeService chatMessageFacadeService() {
            return mock(ChatMessageFacadeService.class);
        }
    }
    private StompSession connect(String authorizationHeader) throws Exception {

        StompHeaders connectHeaders = new StompHeaders();

        if (authorizationHeader != null) {
            connectHeaders.add(
                    "Authorization",
                    authorizationHeader
            );
        }

        String url =
                "http://localhost:"
                        + port
                        + "/api/ws-chat";

        StompSession session = stompClient
                .connectAsync(
                        url,
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        }
                )
                .get(
                        3,
                        TimeUnit.SECONDS
                );

        sessions.add(session);

        return session;
    }

    private void subscribeRoom(
            StompSession session,
            Long roomId,
            BlockingQueue<ChatMessageResponse> queue
    ) {

        session.subscribe(
                "/user/api/sub/chat/rooms/" + roomId,
                new StompFrameHandler() {

                    @Override
                    public Type getPayloadType(
                            StompHeaders headers
                    ) {
                        return ChatMessageResponse.class;
                    }

                    @Override
                    public void handleFrame(
                            StompHeaders headers,
                            Object payload
                    ) {
                        queue.offer(
                                (ChatMessageResponse) payload
                        );
                    }
                }
        );
    }




    @Test
    @DisplayName(
            "STOMP 통합 - Authorization 헤더가 없으면 CONNECT에 실패한다"
    )
    void connectShouldFailWhenAuthorizationHeaderIsMissing() {

        assertThatThrownBy(
                () -> connect(null)
        )
                .isInstanceOf(Exception.class);

        verifyNoInteractions(
                jwtTokenProvider,
                authorizationService
        );
    }

    @Test
    @DisplayName(
            "STOMP 통합 - 유효하지 않은 JWT이면 CONNECT에 실패한다"
    )
    void connectShouldFailWhenJwtIsInvalid() {

        String invalidToken =
                "invalid-integration-token";

        when(
                jwtTokenProvider
                        .validateToken(invalidToken)
        ).thenReturn(false);


        assertThatThrownBy(
                () ->
                        connect(
                                "Bearer "
                                        + invalidToken
                        )
        )
                .isInstanceOf(Exception.class);


        verify(
                jwtTokenProvider
        ).validateToken(
                invalidToken
        );

        verify(
                jwtTokenProvider,
                never()
        ).getUserId(
                anyString()
        );

        verifyNoInteractions(
                authorizationService
        );
    }

    @Test
    @DisplayName(
            "STOMP 통합 - 특정 사용자 destination 메시지는 해당 사용자 세션에서만 수신한다"
    )
    void messageShouldBeDeliveredOnlyToTargetUserSession()
            throws Exception {

        // given
        Long senderUserId = 1L;
        Long receiverUserId = 2L;
        Long roomId = 10L;

        String senderToken = "sender-token";
        String receiverToken = "receiver-token";

        String senderUuid = "sender-uuid";
        String receiverUuid = "receiver-uuid";


        // Sender 인증
        when(
                jwtTokenProvider.validateToken(senderToken)
        ).thenReturn(true);

        when(
                jwtTokenProvider.getUserId(senderToken)
        ).thenReturn(senderUuid);

        when(
                authorizationService
                        .getAuthorizationUserByUserId(senderUuid)
        ).thenReturn(
                new AuthenticatedUser(
                        senderUserId,
                        "ROLE_USER"
                )
        );


        // Receiver 인증
        when(
                jwtTokenProvider.validateToken(receiverToken)
        ).thenReturn(true);

        when(
                jwtTokenProvider.getUserId(receiverToken)
        ).thenReturn(receiverUuid);

        when(
                authorizationService
                        .getAuthorizationUserByUserId(receiverUuid)
        ).thenReturn(
                new AuthenticatedUser(
                        receiverUserId,
                        "ROLE_USER"
                )
        );


        /*
         * Sender가 메시지를 보내면
         * 실제 비즈니스 로직 대신 Receiver user destination으로 전달한다.
         *
         * 이번 테스트 목적은 Facade의 수신자 선정이 아니라
         * Spring STOMP의 사용자별 세션 라우팅 검증이다.
         */
        doAnswer(invocation -> {

            ChatSendRequest request =
                    invocation.getArgument(0);

            Long authenticatedUserId =
                    invocation.getArgument(1);

            assertThat(authenticatedUserId)
                    .isEqualTo(senderUserId);


            ChatMessageResponse response =
                    new ChatMessageResponse(
                            1000L,
                            request.roomId(),
                            request.clientMessageId(),
                            request.messageType().name(),
                            new ChatMessageSenderResponse(
                                    senderUuid,
                                    "sender",
                                    null
                            ),
                            request.messageContent(),
                            LocalDateTime.now(),
                            false,
                            1
                    );


            String receiverDestination =
                    "/user/"
                            + receiverUserId
                            + "/api/sub/chat/rooms/"
                            + request.roomId();


            messagingTemplate.convertAndSend(
                    receiverDestination,
                    response
            );

            return null;

        }).when(chatMessageFacadeService)
                .sendMessage(
                        any(ChatSendRequest.class),
                        eq(senderUserId)
                );


        BlockingQueue<ChatMessageResponse> senderQueue =
                new LinkedBlockingQueue<>();

        BlockingQueue<ChatMessageResponse> receiverQueue =
                new LinkedBlockingQueue<>();


        // Sender / Receiver 실제 STOMP 연결
        StompSession senderSession =
                connect(
                        "Bearer "
                                + senderToken
                );

        StompSession receiverSession =
                connect(
                        "Bearer "
                                + receiverToken
                );


        // 동일한 room destination 구독
        subscribeRoom(
                senderSession,
                roomId,
                senderQueue
        );

        subscribeRoom(
                receiverSession,
                roomId,
                receiverQueue
        );


        Thread.sleep(200);


        // when
        ChatSendRequest request =
                new ChatSendRequest(
                        roomId,
                        MessageType.TEXT,
                        "TWO_USER_STOMP_TEST",
                        "client-two-user-001"
                );

        senderSession.send(
                "/api/pub/chat/message",
                request
        );


        // then
        ChatMessageResponse receiverMessage =
                receiverQueue.poll(
                        3,
                        TimeUnit.SECONDS
                );


        assertThat(receiverMessage)
                .isNotNull();

        assertThat(receiverMessage.content())
                .isEqualTo(
                        "TWO_USER_STOMP_TEST"
                );

        assertThat(receiverMessage.clientMessageId())
                .isEqualTo(
                        "client-two-user-001"
                );

        assertThat(receiverMessage.roomId())
                .isEqualTo(roomId);


        /*
         * userId=2로 전송했으므로
         * userId=1 세션에서는 수신하면 안 된다.
         */
        ChatMessageResponse senderMessage =
                senderQueue.poll(
                        500,
                        TimeUnit.MILLISECONDS
                );

        assertThat(senderMessage)
                .isNull();


        verify(
                chatMessageFacadeService,
                timeout(1000)
        ).sendMessage(
                argThat(message ->
                        message.roomId().equals(roomId)
                                && message.messageContent()
                                .equals("TWO_USER_STOMP_TEST")
                ),
                eq(senderUserId)
        );
    }

}