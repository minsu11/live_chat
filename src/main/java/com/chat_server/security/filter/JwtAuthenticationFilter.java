package com.chat_server.security.filter;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.security.provider.JwtTokenProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import com.chat_server.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthorizationService authorizationService;

    private static final AntPathMatcher pm = new AntPathMatcher();
    private static final String[] SKIP = {
        "/api/ws/**", "/api/ws-chat/**", "/api/ws-chat/info**",
        "/api/sockjs/**", "/api/webjars/**", "/api/favicon.ico", "/api/v1/users/login"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String p : SKIP) if (pm.match(p, path)) return true;
        return false;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.info("http 인가 처리");
        log.info("api 경로: {}", request.getRequestURI());
        if(shouldNotFilter(request)){
            log.info("스킵 api 경로");
            filterChain.doFilter(request, response);
        }

        String token = CookieUtil.getCookie(request,"accessToken");
        log.info("token 값 가지고 오기");
        if (token != null && jwtTokenProvider.validateToken(token)) {
            log.info("if문 안");
            String userId = jwtTokenProvider.getUserId(token);
            log.info("userId: {}", userId);
            AuthenticatedUser authenticatedUser =
                    authorizationService.getAuthorizationUserByUserId(userId);
            log.info("authentication after");
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(authenticatedUser.role()));

            setAuthenticationIfRequired(authenticatedUser, authorities);
            log.info("set Authentication");
        } else {
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }


    private void setAuthenticationIfRequired(AuthenticatedUser authenticatedUser,
                                             List<SimpleGrantedAuthority> authorities) {
        var context = SecurityContextHolder.getContext();
        var existingAuth = context.getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            var authentication =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            context.setAuthentication(authentication);
        }
    }
}
