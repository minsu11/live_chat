package com.chat_server.security.filter;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.security.provider.JwtTokenProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthorizationService authorizationService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authorizationService = mock(AuthorizationService.class);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, authorizationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * WebSocket handshake와 로그인처럼 필터 제외 대상으로 등록한 경로가
     * JWT 인증 필터를 건너뛰도록 판정되는지 검증한다.
     */
    @Test
    @DisplayName("HTTP JWT 필터 제외 성공 - WebSocket 및 로그인 경로는 인증 필터 대상에서 제외한다")
    void shouldNotFilterShouldReturnTrueForExcludedPaths() {
        MockHttpServletRequest websocketRequest = new MockHttpServletRequest("GET", "/api/ws-chat/info");
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/api/v1/users/login");
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/v1/chat-list");

        assertThat(filter.shouldNotFilter(websocketRequest)).isTrue();
        assertThat(filter.shouldNotFilter(loginRequest)).isTrue();
        assertThat(filter.shouldNotFilter(apiRequest)).isFalse();
    }

    /**
     * 유효한 accessToken 쿠키가 전달되면 토큰의 사용자 식별자를 조회하고,
     * 인가 정보를 기반으로 SecurityContext에 인증 객체를 저장하는지 검증한다.
     */
    @Test
    @DisplayName("HTTP JWT 인증 성공 - 유효한 쿠키 토큰이면 인증 사용자와 권한을 SecurityContext에 저장한다")
    void doFilterInternalShouldSetAuthenticationWhenTokenIsValid() throws Exception {
        MockHttpServletRequest request = requestWithAccessToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(10L, "ROLE_USER");

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn("user-uuid");
        when(authorizationService.getAuthorizationUserByUserId("user-uuid"))
                .thenReturn(authenticatedUser);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(authenticatedUser);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(request.getAttribute("exception")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    /**
     * accessToken 쿠키가 없는 요청은 인증 객체를 만들지 않고 INVALID_TOKEN을 기록하는지 검증한다.
     * 토큰이 없으므로 JwtTokenProvider와 AuthorizationService는 호출하지 않아야 한다.
     */
    @Test
    @DisplayName("HTTP JWT 인증 실패 - accessToken 쿠키가 없으면 INVALID_TOKEN을 기록한다")
    void doFilterInternalShouldMarkInvalidTokenWhenCookieIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chat-list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("exception")).isEqualTo(ErrorCode.INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider, authorizationService);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 쿠키는 존재하지만 토큰 검증에 실패한 경우를 검증한다.
     * 실패한 토큰으로 사용자 정보를 조회하거나 인증 객체를 만들면 안 된다.
     */
    @Test
    @DisplayName("HTTP JWT 인증 실패 - 토큰 검증에 실패하면 사용자 조회 없이 INVALID_TOKEN을 기록한다")
    void doFilterInternalShouldMarkInvalidTokenWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = requestWithAccessToken("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("exception")).isEqualTo(ErrorCode.INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).getUserId(anyString());
        verifyNoInteractions(authorizationService);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 이미 인증된 사용자가 SecurityContext에 존재할 때 새 토큰의 인증 정보로 덮어쓰지 않는지 검증한다.
     * 동일 요청 처리 과정에서 앞선 인증 결과를 보존해야 한다.
     */
    @Test
    @DisplayName("HTTP JWT 인증 경계값 - 기존 인증이 유효하면 새 인증 객체로 덮어쓰지 않는다")
    void doFilterInternalShouldKeepExistingAuthenticatedPrincipal() throws Exception {
        var existingAuthentication = new UsernamePasswordAuthenticationToken(
                "existing-user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        MockHttpServletRequest request = requestWithAccessToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        AuthenticatedUser newUser = new AuthenticatedUser(20L, "ROLE_USER");

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn("new-user-uuid");
        when(authorizationService.getAuthorizationUserByUserId("new-user-uuid"))
                .thenReturn(newUser);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithAccessToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chat-list");
        request.setCookies(new Cookie("accessToken", token));
        return request;
    }
}
