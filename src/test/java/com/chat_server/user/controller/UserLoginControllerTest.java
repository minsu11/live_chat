package com.chat_server.user.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import com.chat_server.user.service.UserLoginService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserLoginControllerTest {
    @Test
    @DisplayName("로그인 성공 시 accessToken 쿠키를 설정하고 로그인 응답을 반환한다")
    void loginShouldSetAccessTokenCookie() {
        UserLoginService service = mock(UserLoginService.class);
        UserLoginController controller = new UserLoginController(service, customProperties("X-Token-Expires-At"));
        LoginRequest request = new LoginRequest("input01", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Token-Expires-At", ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).toString());
        when(service.login(request)).thenReturn(ResponseEntity.ok().headers(headers).body(ApiResponse.success(200, new LoginTokenResponse("access-token", null))));
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        var response = controller.login(request, servletResponse);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().accessToken()).isEqualTo("access-token");
        verify(servletResponse).setHeader(eq("Set-Cookie"), contains("accessToken=access-token"));
    }

    @Test
    @DisplayName("로그인 실패 시 만료 헤더가 없으면 쿠키를 만들지 않고 예외를 발생시킨다")
    void loginShouldThrowWhenExpirationHeaderIsMissing() {
        UserLoginService service = mock(UserLoginService.class);
        UserLoginController controller = new UserLoginController(service, customProperties("X-Token-Expires-At"));
        LoginRequest request = new LoginRequest("input01", "password");
        when(service.login(request)).thenReturn(ResponseEntity.ok(ApiResponse.success(200, new LoginTokenResponse("access-token", null))));
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> controller.login(request, servletResponse))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresIn is null");
        verify(servletResponse, never()).setHeader(eq("Set-Cookie"), any());
    }

    private CustomProperties customProperties(String tokenExpirationHeader) {
        CustomProperties properties = new CustomProperties();
        CustomProperties.Auth auth = new CustomProperties.Auth();
        CustomProperties.Auth.Header header = new CustomProperties.Auth.Header();
        header.setTokenExpiration(tokenExpirationHeader);
        auth.setHeader(header);
        properties.setAuth(auth);
        return properties;
    }
}
