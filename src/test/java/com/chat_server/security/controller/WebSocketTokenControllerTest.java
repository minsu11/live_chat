package com.chat_server.security.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebSocketTokenControllerTest {
    @Test
    @DisplayName("웹소켓 토큰 조회 성공 시 accessToken 쿠키 값을 반환한다")
    void getTokenShouldReturnAccessTokenCookie() {
        WebSocketTokenController controller = new WebSocketTokenController();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", "token-value")});

        var response = controller.getToken(request);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo("token-value");
    }

    @Test
    @DisplayName("웹소켓 토큰 조회 성공 시 쿠키가 없으면 null 데이터를 반환한다")
    void getTokenShouldReturnNullWhenCookieIsMissing() {
        WebSocketTokenController controller = new WebSocketTokenController();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        var response = controller.getToken(request);

        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getData()).isNull();
    }
}
